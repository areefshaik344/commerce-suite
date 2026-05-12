import * as Yup from "yup";
import { passwordSchema, phoneSchema, emailSchema } from "./validation";

export const personalDetailsSchema = Yup.object({
  name: Yup.string().trim().required("Name is required").min(2).max(80),
  email: emailSchema,
  phone: phoneSchema,
  gender: Yup.string().oneOf(["male", "female", "other", "prefer_not_to_say", ""]).nullable(),
  dob: Yup.string().nullable().test("dob-valid", "Enter a valid date of birth", (v) => {
    if (!v) return true;
    const d = new Date(v);
    if (isNaN(d.getTime())) return false;
    const age = (Date.now() - d.getTime()) / (365.25 * 24 * 3600 * 1000);
    return age >= 13 && age <= 120;
  }),
  bio: Yup.string().max(280, "Bio must be 280 characters or less"),
});

export const addressSchema = Yup.object({
  name: Yup.string().trim().required("Name is required").max(80),
  phone: Yup.string().required("Phone is required").matches(/^\+?[\d\s-]{10,15}$/, "Enter a valid phone"),
  line1: Yup.string().trim().required("Address line 1 is required").max(120),
  line2: Yup.string().max(120),
  city: Yup.string().trim().required("City is required").max(60),
  state: Yup.string().trim().required("State is required").max(60),
  pincode: Yup.string().required("Pincode is required").matches(/^\d{6}$/, "Enter a valid 6-digit pincode"),
  type: Yup.string().oneOf(["HOME", "WORK", "OTHER"]).required(),
  isDefault: Yup.boolean(),
});

export const changePasswordSchema = Yup.object({
  currentPassword: Yup.string().required("Enter your current password"),
  newPassword: passwordSchema.notOneOf([Yup.ref("currentPassword")], "New password must differ from current"),
  confirmPassword: Yup.string().required("Confirm your new password").oneOf([Yup.ref("newPassword")], "Passwords must match"),
});

export const vendorBusinessSchema = Yup.object({
  businessName: Yup.string().trim().required("Business name is required").max(120),
  legalName: Yup.string().trim().required("Legal name is required").max(120),
  storeSlug: Yup.string().trim().required("Store slug is required").matches(/^[a-z0-9-]{3,40}$/, "Lowercase letters, numbers, hyphens (3-40 chars)"),
  supportEmail: emailSchema,
  supportPhone: Yup.string().required("Support phone is required").matches(/^\+?[\d\s-]{10,15}$/, "Enter a valid phone"),
  description: Yup.string().max(500),
  category: Yup.string().required("Category is required"),
  gstin: Yup.string().required("GSTIN is required").matches(/^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9][A-Z][0-9A-Z]$/, "Enter a valid GSTIN"),
  pan: Yup.string().required("PAN is required").matches(/^[A-Z]{5}[0-9]{4}[A-Z]$/, "Enter a valid PAN"),
});

export const AVATAR_MAX_BYTES = 2 * 1024 * 1024; // 2 MB
export const AVATAR_ACCEPTED = ["image/jpeg", "image/png", "image/webp"];

export interface AvatarValidationError { code: "TYPE" | "SIZE" | "READ"; message: string; }
export function validateAvatarFile(file: File): AvatarValidationError | null {
  if (!AVATAR_ACCEPTED.includes(file.type)) return { code: "TYPE", message: "Only JPG, PNG or WEBP images are allowed" };
  if (file.size > AVATAR_MAX_BYTES) return { code: "SIZE", message: "Image must be smaller than 2 MB" };
  return null;
}