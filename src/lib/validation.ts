import * as Yup from "yup";

export const PASSWORD_REGEX = {
  upper: /[A-Z]/,
  lower: /[a-z]/,
  digit: /\d/,
  symbol: /[^A-Za-z0-9]/,
};

export const passwordSchema = Yup.string()
  .required("Password is required")
  .min(8, "Must be at least 8 characters")
  .matches(PASSWORD_REGEX.upper, "Must contain an uppercase letter")
  .matches(PASSWORD_REGEX.lower, "Must contain a lowercase letter")
  .matches(PASSWORD_REGEX.digit, "Must contain a number")
  .matches(PASSWORD_REGEX.symbol, "Must contain a symbol");

export const emailSchema = Yup.string()
  .required("Email is required")
  .email("Enter a valid email address")
  .max(255);

export const phoneSchema = Yup.string()
  .required("Phone is required")
  .matches(/^\d{10}$/, "Enter a valid 10-digit phone number");

export const loginSchema = Yup.object({
  email: emailSchema,
  password: Yup.string().required("Password is required"),
  rememberMe: Yup.boolean(),
});

export const signupSchema = Yup.object({
  name: Yup.string().trim().required("Full name is required").min(2).max(80),
  email: emailSchema,
  phone: phoneSchema,
  password: passwordSchema,
  confirmPassword: Yup.string()
    .required("Please confirm your password")
    .oneOf([Yup.ref("password")], "Passwords must match"),
  acceptTerms: Yup.boolean().oneOf([true], "You must accept the Terms & Conditions"),
});

export const forgotPasswordSchema = Yup.object({
  email: emailSchema,
});

export const resetPasswordSchema = Yup.object({
  password: passwordSchema,
  confirmPassword: Yup.string()
    .required("Please confirm your password")
    .oneOf([Yup.ref("password")], "Passwords must match"),
});

export const otpSchema = Yup.object({
  otp: Yup.string()
    .required("OTP is required")
    .matches(/^\d{6}$/, "OTP must be 6 digits"),
});