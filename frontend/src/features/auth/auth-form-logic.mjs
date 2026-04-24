import { ApiError } from "../../lib/api/runtime.mjs";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const SERVER_ERROR_MESSAGE = "잠시 후 다시 시도해주세요.";

export function getPostAuthRedirectPath(user) {
  return user.coupleId ? "/today" : "/couple";
}

export function validateLoginValues(values) {
  const errors = {};
  const email = values.email.trim();

  if (!email) {
    errors.email = "이메일을 입력해주세요.";
  } else if (!EMAIL_PATTERN.test(email)) {
    errors.email = "올바른 이메일 형식이 아닙니다.";
  }

  if (!values.password) {
    errors.password = "비밀번호를 입력해주세요.";
  }

  return errors;
}

export function validateSignupValues(values) {
  const errors = validateLoginValues(values);
  const displayName = values.displayName.trim();

  if (!displayName) {
    errors.displayName = "표시 이름을 입력해주세요.";
  } else if (displayName.length > 20) {
    errors.displayName = "표시 이름은 20자 이하로 입력해주세요.";
  }

  if (!values.password) {
    errors.password = "비밀번호를 입력해주세요.";
  } else if (values.password.length < 8 || values.password.length > 72) {
    errors.password = "비밀번호는 8자 이상 72자 이하로 입력해주세요.";
  }

  return errors;
}

function hasFieldErrors(errors) {
  return Object.values(errors).some(Boolean);
}

function mapApiError(error) {
  if (!(error instanceof ApiError)) {
    return { ok: false, fieldErrors: {}, formError: SERVER_ERROR_MESSAGE };
  }

  if (error.code === "VALIDATION_ERROR") {
    const fieldErrors = Object.fromEntries(error.fields.map((field) => [field.field, field.message]));

    return {
      ok: false,
      fieldErrors,
      formError: hasFieldErrors(fieldErrors) ? undefined : error.message,
    };
  }

  if (error.code === "LOGIN_FAILED") {
    return { ok: false, fieldErrors: {}, formError: error.message };
  }

  return { ok: false, fieldErrors: {}, formError: SERVER_ERROR_MESSAGE };
}

export async function submitLogin(values, deps) {
  const payload = {
    email: values.email.trim(),
    password: values.password,
  };
  const fieldErrors = validateLoginValues(payload);

  if (hasFieldErrors(fieldErrors)) {
    return { ok: false, fieldErrors };
  }

  try {
    const user = await deps.login(payload);
    return { ok: true, redirectTo: getPostAuthRedirectPath(user) };
  } catch (error) {
    return mapApiError(error);
  }
}

export async function submitSignup(values, deps) {
  const payload = {
    email: values.email.trim(),
    password: values.password,
    displayName: values.displayName.trim(),
  };
  const fieldErrors = validateSignupValues(payload);

  if (hasFieldErrors(fieldErrors)) {
    return { ok: false, fieldErrors };
  }

  try {
    await deps.signup(payload);
    const user = await deps.getCurrentUser();
    return { ok: true, redirectTo: getPostAuthRedirectPath(user) };
  } catch (error) {
    return mapApiError(error);
  }
}
