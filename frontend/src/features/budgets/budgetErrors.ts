import axios from "axios";

export function isNotFound(error: unknown) {
  return axios.isAxiosError(error) && error.response?.status === 404;
}

export function isBudgetYearAlreadyExistsError(error: unknown) {
  return (
    axios.isAxiosError(error) &&
    (error.response?.status === 409 || error.response?.status === 400) &&
    error.response?.data?.message === "Budget year already exists"
  );
}

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    return (
      error.response?.data?.message ||
      error.response?.data?.error ||
      "Ocurrió un error inesperado."
    );
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Ocurrió un error inesperado.";
}
