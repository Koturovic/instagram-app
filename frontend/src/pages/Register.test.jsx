import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Register from './Register';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate
  };
});

vi.mock('../services/authService', () => ({
  register: vi.fn()
}));

import { register } from '../services/authService';

describe('Register Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderRegister = () => {
    return render(
      <BrowserRouter>
        <Register />
      </BrowserRouter>
    );
  };

  it('should render register form', () => {
    renderRegister();

    expect(screen.getByText('Create an Instagram account')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('First name')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Last name')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Username')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Email')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign up/i })).toBeInTheDocument();
  });

  it('should show error for invalid username format', async () => {
    renderRegister();

    const usernameInput = screen.getByPlaceholderText('Username');
    
    fireEvent.change(usernameInput, { target: { value: 'ab' } }); // Too short
    fireEvent.blur(usernameInput);

    await waitFor(() => {
      expect(screen.getByText(/Username must be 3-20 chars/i)).toBeInTheDocument();
    });
  });

  it('should show error for taken username', async () => {
    renderRegister();

    const usernameInput = screen.getByPlaceholderText('Username');
    
    fireEvent.change(usernameInput, { target: { value: 'admin' } }); // Taken username
    fireEvent.blur(usernameInput);

    await waitFor(() => {
      expect(screen.getByText('Username already taken')).toBeInTheDocument();
    });
  });

  it('should validate all required fields on submit', async () => {
    renderRegister();

    const submitButton = screen.getByRole('button', { name: /sign up/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('First name is required')).toBeInTheDocument();
    });
  });

  it('should show error for invalid email', async () => {
    renderRegister();

    const firstNameInput = screen.getByPlaceholderText('First name');
    const lastNameInput = screen.getByPlaceholderText('Last name');
    const usernameInput = screen.getByPlaceholderText('Username');
    const emailInput = screen.getByPlaceholderText('Email');
    const passwordInput = screen.getByPlaceholderText('Password');
    const submitButton = screen.getByRole('button', { name: /sign up/i });

    fireEvent.change(firstNameInput, { target: { value: 'John' } });
    fireEvent.change(lastNameInput, { target: { value: 'Doe' } });
    fireEvent.change(usernameInput, { target: { value: 'johndoe' } });
    fireEvent.change(emailInput, { target: { value: 'invalidemail' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    
    fireEvent.click(submitButton);

    const errorMessage = await screen.findByText('Invalid email format');
    expect(errorMessage).toBeInTheDocument();
  });

  it('should show error for short password', async () => {
    renderRegister();

    const firstNameInput = screen.getByPlaceholderText('First name');
    const lastNameInput = screen.getByPlaceholderText('Last name');
    const usernameInput = screen.getByPlaceholderText('Username');
    const emailInput = screen.getByPlaceholderText('Email');
    const passwordInput = screen.getByPlaceholderText('Password');
    const submitButton = screen.getByRole('button', { name: /sign up/i });

    fireEvent.change(firstNameInput, { target: { value: 'John' } });
    fireEvent.change(lastNameInput, { target: { value: 'Doe' } });
    fireEvent.change(usernameInput, { target: { value: 'johndoe' } });
    fireEvent.change(emailInput, { target: { value: 'john@example.com' } });
    fireEvent.change(passwordInput, { target: { value: '12345' } });
    
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Password must be at least 6 characters')).toBeInTheDocument();
    });
  });

  it('should call register service on valid form submission', async () => {
    register.mockResolvedValue({ success: true });
    window.alert = vi.fn();

    renderRegister();

    const firstNameInput = screen.getByPlaceholderText('First name');
    const lastNameInput = screen.getByPlaceholderText('Last name');
    const usernameInput = screen.getByPlaceholderText('Username');
    const emailInput = screen.getByPlaceholderText('Email');
    const passwordInput = screen.getByPlaceholderText('Password');
    const submitButton = screen.getByRole('button', { name: /sign up/i });

    fireEvent.change(firstNameInput, { target: { value: 'John' } });
    fireEvent.change(lastNameInput, { target: { value: 'Doe' } });
    fireEvent.change(usernameInput, { target: { value: 'johndoe123' } });
    fireEvent.change(emailInput, { target: { value: 'john@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(register).toHaveBeenCalledWith({
        firstName: 'John',
        lastName: 'Doe',
        username: 'johndoe123',
        email: 'john@example.com',
        password: 'password123'
      });
      expect(window.alert).toHaveBeenCalledWith('Registration successful!');
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });

  it('should show error on failed registration', async () => {
    register.mockRejectedValue(new Error('Registration failed'));
    window.alert = vi.fn();

    renderRegister();

    const firstNameInput = screen.getByPlaceholderText('First name');
    const lastNameInput = screen.getByPlaceholderText('Last name');
    const usernameInput = screen.getByPlaceholderText('Username');
    const emailInput = screen.getByPlaceholderText('Email');
    const passwordInput = screen.getByPlaceholderText('Password');
    const submitButton = screen.getByRole('button', { name: /sign up/i });

    fireEvent.change(firstNameInput, { target: { value: 'John' } });
    fireEvent.change(lastNameInput, { target: { value: 'Doe' } });
    fireEvent.change(usernameInput, { target: { value: 'johndoe123' } });
    fireEvent.change(emailInput, { target: { value: 'john@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledWith('Registration failed! Please try again.');
    });
  });
});
