import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Login from './Login';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate
  };
});

vi.mock('../services/authService', () => ({
  login: vi.fn()
}));

import { login } from '../services/authService';

describe('Login Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  const renderLogin = () => {
    return render(
      <BrowserRouter>
        <Login />
      </BrowserRouter>
    );
  };

  it('should render login form', () => {
    renderLogin();

    expect(screen.getByText('Instagram')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Enter email')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Enter password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /log in/i })).toBeInTheDocument();
  });

  it('should show error for invalid email format', async () => {
    renderLogin();

    const emailInput = screen.getByPlaceholderText('Enter email');
    const passwordInput = screen.getByPlaceholderText('Enter password');
    const submitButton = screen.getByRole('button', { name: /log in/i });

    fireEvent.change(emailInput, { target: { value: 'invalidemail' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    fireEvent.click(submitButton);

    const errorMessage = await screen.findByText('Invalid email format');
    expect(errorMessage).toBeInTheDocument();
  });

  it('should show error for short password', async () => {
    renderLogin();

    const emailInput = screen.getByPlaceholderText('Enter email');
    const passwordInput = screen.getByPlaceholderText('Enter password');
    const submitButton = screen.getByRole('button', { name: /log in/i });

    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    fireEvent.change(passwordInput, { target: { value: '12345' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Password must be at least 6 characters')).toBeInTheDocument();
    });
  });

  it('should toggle password visibility', () => {
    renderLogin();

    const passwordInput = screen.getByPlaceholderText('Enter password');
    const toggleButton = screen.getByRole('button', { name: /show/i });

    expect(passwordInput).toHaveAttribute('type', 'password');

    fireEvent.click(toggleButton);
    expect(passwordInput).toHaveAttribute('type', 'text');
    expect(screen.getByRole('button', { name: /hide/i })).toBeInTheDocument();

    fireEvent.click(toggleButton);
    expect(passwordInput).toHaveAttribute('type', 'password');
  });

  it('should call login service and navigate on successful login', async () => {
    const mockToken = 'mock.jwt.token';
    login.mockResolvedValue({ token: mockToken });

    window.alert = vi.fn();

    renderLogin();

    const emailInput = screen.getByPlaceholderText('Enter email');
    const passwordInput = screen.getByPlaceholderText('Enter password');
    const submitButton = screen.getByRole('button', { name: /log in/i });

    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(login).toHaveBeenCalledWith('test@example.com', 'password123');
      expect(localStorage.setItem).toHaveBeenCalledWith('token', mockToken);
      expect(window.alert).toHaveBeenCalledWith('Login success!');
      expect(mockNavigate).toHaveBeenCalledWith('/home');
    });
  });

  it('should show error on failed login', async () => {
    login.mockRejectedValue(new Error('Invalid credentials'));

    renderLogin();

    const emailInput = screen.getByPlaceholderText('Enter email');
    const passwordInput = screen.getByPlaceholderText('Enter password');
    const submitButton = screen.getByRole('button', { name: /log in/i });

    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'wrongpassword' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Wrong email or password')).toBeInTheDocument();
    });
  });

  it('should navigate to register page when clicking create account button', () => {
    renderLogin();

    const registerButton = screen.getByRole('button', { name: /create a new account/i });
    fireEvent.click(registerButton);

    expect(mockNavigate).toHaveBeenCalledWith('/register');
  });
});
