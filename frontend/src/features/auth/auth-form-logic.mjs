import { ApiError } from "../../lib/api/runtime.mjs";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const SERVER_ERROR_MESSAGE = "\uc7a0\uc2dc \ud6c4 \ub2e4\uc2dc \uc2dc\ub3c4\ud574\uc8fc\uc138\uc694.";

export function getPostAuthRedirectPath(user) {
  return user.coupleId ? "/today" : "/couple";
}

export function validateLoginValues(values) {
  const errors = {};
  const email = values.email.trim();

  if (!email) {
    errors.email = "\uc774\uba54\uc77c\uc744 \uc785\ub825\ud574\uc8fc\uc138\uc694.";
  } else if (!EMAIL_PATTERN.test(email)) {
    errors.email = "\uc62c\ubc14\ub978 \uc774\uba54\uc77c \ud615\uc2dd\uc774 \uc544\ub2d9\ub2c8\ub2e4.";
  }

  if (!values.password) {
    errors.password = "\ube44\ubc00\ubc88\ud638\ub97c \uc785\ub825\ud574\uc8fc\uc138\uc694.";
  }

  return errors;
}

export function validateSignupValues(values) {
  const errors = validateLoginValues(values);
  const displayName = values.displayName.trim();

  if (!displayName) {
    errors.displayName = "\ud45c\uc2dc \uc774\ub984\uc744 \uc785\ub825\ud574\uc8fc\uc138\uc694.";
  } else if (displayName.length > 20) {
    errors.displayName = "\ud45c\uc2dc \uc774\ub984\uc740 20\uc790 \uc774\ud558\ub85c \uc785\ub825\ud574\uc8fc\uc138\uc694.";
  }

  if (!values.password) {
    errors.password = "\ube44\ubc00\ubc88\ud638\ub97c \uc785\ub825\ud574\uc8fc\uc138\uc694.";
  } else if (values.password.length < 8 || values.password.length > 72) {
    errors.password = "\ube44\ubc00\ubc88\ud638\ub294 8\uc790 \uc774\uc0c1 72\uc790 \uc774\ud558\ub85c \uc785\ub825\ud574\uc8fc\uc138\uc694.";
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
