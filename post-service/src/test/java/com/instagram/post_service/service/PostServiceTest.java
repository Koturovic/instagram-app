package com.instagram.post_service.service;

import com.instagram.post_service.entity.Post;
import com.instagram.post_service.entity.PostMedia;
import com.instagram.post_service.repository.PostRepository;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private PostService postService;

    @Test
    void createPostWithMedia_savesPostWithMedia() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "photo.jpg",
                "image/jpeg",
                "data".getBytes(StandardCharsets.UTF_8)
        );

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        doNothing().when(minioClient).setBucketPolicy(any(SetBucketPolicyArgs.class));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.createPostWithMedia("desc", 7L, List.<MultipartFile>of(file));

        assertNotNull(result);
        assertEquals(7L, result.getUserId());
        assertEquals("desc", result.getDescription());
        assertEquals(1, result.getMediaFiles().size());
        assertTrue(result.getMediaFiles().get(0).getFileUrl().contains("/instagram-media/"));
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void getAllPosts_returnsPostsOrderedByCreatedAtDesc() {
        Post post1 = Post.builder().id(1L).createdAt(java.time.LocalDateTime.now().minusDays(1)).build();
        Post post2 = Post.builder().id(2L).createdAt(java.time.LocalDateTime.now()).build();

        when(postRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(post2, post1));

        List<Post> result = postService.getAllPosts();

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getId());
        verify(postRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getPostsByUserId_returnsUserPostsOrderedByCreatedAtDesc() {
        Post post1 = Post.builder().id(1L).userId(5L).createdAt(java.time.LocalDateTime.now().minusDays(1)).build();
        Post post2 = Post.builder().id(2L).userId(5L).createdAt(java.time.LocalDateTime.now()).build();

        when(postRepository.findByUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(post2, post1));

        List<Post> result = postService.getPostsByUserId(5L);

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getId());
        verify(postRepository).findByUserIdOrderByCreatedAtDesc(5L);
    }

    @Test
    void createPostWithMedia_throwsWhenTooManyFiles() {
        List<MultipartFile> files = java.util.stream.IntStream.range(0, 21)
                .mapToObj(i -> (MultipartFile) new MockMultipartFile("files", "f" + i + ".jpg", "image/jpeg", new byte[] {1}))
                .toList();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> postService.createPostWithMedia("desc", 1L, files));
        assertTrue(ex.getMessage().contains("20"));
    }

    @Test
    void createPostWithMedia_throwsWhenFileTooLarge() throws Exception {
        byte[] big = new byte[50 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("files", "big.jpg", "image/jpeg", big);

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        doNothing().when(minioClient).setBucketPolicy(any(SetBucketPolicyArgs.class));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> postService.createPostWithMedia("desc", 1L, List.<MultipartFile>of(file)));
        assertTrue(ex.getMessage().contains("50MB"));
    }

    @Test
    void deletePost_removesMediaAndDeletesPost() throws Exception {
        Post post = Post.builder()
                .id(5L)
                .userId(9L)
                .mediaFiles(List.of(
                        PostMedia.builder().id(1L).fileUrl("http://localhost:9000/instagram-media/a.jpg").build(),
                        PostMedia.builder().id(2L).fileUrl("http://localhost:9000/instagram-media/b.jpg").build()
                ))
                .build();

        when(postRepository.findById(5L)).thenReturn(Optional.of(post));
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

        postService.deletePost(5L);

        verify(minioClient, times(2)).removeObject(any(RemoveObjectArgs.class));
        verify(postRepository).delete(post);
    }

    @Test
    void updatePost_replacesMediaWhenFilesProvided() throws Exception {
        Post existing = Post.builder()
                .id(1L)
                .userId(2L)
                .mediaFiles(new java.util.ArrayList<>(List.of(
                        PostMedia.builder().id(10L).fileUrl("http://localhost:9000/instagram-media/old.jpg").build()
                )))
                .build();

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "new.jpg",
                "image/jpeg",
                "data".getBytes(StandardCharsets.UTF_8)
        );

        when(postRepository.findById(1L)).thenReturn(Optional.of(existing));
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post updated = postService.updatePost(1L, "new desc", List.<MultipartFile>of(file));

        assertEquals("new desc", updated.getDescription());
        assertEquals(1, updated.getMediaFiles().size());
        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void deleteMediaFromPost_removesMediaWhenOwner() throws Exception {
        PostMedia media = PostMedia.builder()
                .id(3L)
                .fileUrl("http://localhost:9000/instagram-media/old.jpg")
                .build();
        Post post = Post.builder()
                .id(1L)
                .userId(7L)
                .mediaFiles(new java.util.ArrayList<>(List.of(
                        media,
                        PostMedia.builder().id(4L).fileUrl("http://localhost:9000/instagram-media/keep.jpg").build()
                )))
                .build();

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post updated = postService.deleteMediaFromPost(1L, 3L, 7L);

        assertEquals(1, updated.getMediaFiles().size());
        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void deleteMediaFromPost_throwsWhenNotOwner() {
        Post post = Post.builder()
                .id(1L)
                .userId(7L)
                .mediaFiles(new java.util.ArrayList<>(List.of(
                        PostMedia.builder().id(3L).fileUrl("http://localhost:9000/instagram-media/old.jpg").build(),
                        PostMedia.builder().id(4L).fileUrl("http://localhost:9000/instagram-media/keep.jpg").build()
                )))
                .build();

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> postService.deleteMediaFromPost(1L, 3L, 99L));
        assertTrue(ex.getMessage().toLowerCase().contains("nemate"));
    }
}
