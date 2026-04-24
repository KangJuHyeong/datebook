import { ApiError } from "../../lib/api/runtime.mjs";

const SERVER_ERROR_MESSAGE = "\uc7a0\uc2dc \ud6c4 \ub2e4\uc2dc \uc2dc\ub3c4\ud574\uc8fc\uc138\uc694.";

export function validateInviteCode(values) {
  const inviteCode = values.inviteCode.trim();

  if (!inviteCode) {
    return { inviteCode: "\ucd08\ub300 \ucf54\ub4dc\ub97c \uc785\ub825\ud574\uc8fc\uc138\uc694." };
  }

  return {};
}

function hasErrors(errors) {
  return Object.values(errors).some(Boolean);
}

export async function submitCreateInvite(deps) {
  try {
    const invite = await deps.createCouple();
    return { ok: true, invite };
  } catch (error) {
    if (error instanceof ApiError) {
      return { ok: false, message: error.message };
    }

    return { ok: false, message: SERVER_ERROR_MESSAGE };
  }
}

export async function submitJoinInvite(values, deps) {
  const payload = { inviteCode: values.inviteCode.trim() };
  const fieldErrors = validateInviteCode(payload);

  if (hasErrors(fieldErrors)) {
    return { ok: false, fieldErrors };
  }

  try {
    const response = await deps.joinCouple(payload);
    return { ok: true, response, redirectTo: "/today" };
  } catch (error) {
    if (error instanceof ApiError) {
      if (error.code === "VALIDATION_ERROR") {
        const apiFieldErrors = Object.fromEntries(error.fields.map((field) => [field.field, field.message]));
        return {
          ok: false,
          fieldErrors: apiFieldErrors,
          formError: hasErrors(apiFieldErrors) ? undefined : error.message,
        };
      }

      return { ok: false, fieldErrors: {}, formError: error.message };
    }

    return { ok: false, fieldErrors: {}, formError: SERVER_ERROR_MESSAGE };
  }
}
