import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { getApiErrorMessage } from '../api/http';
import { previewMonthlyPlanQuickCapture, commitMonthlyPlanQuickCapture } from '../api/monthlyPlanQuickCaptureApi';
import { QuickCapturePreviewResponse, MonthlyPlanItemCreatePayload } from '../domain/types';

type Params = {
  profileId: string;
  year: number;
  month: number;
  invalidatePlanningViews: () => void;
};

export function useQuickCaptureFlow({
  profileId,
  year,
  month,
  invalidatePlanningViews,
}: Params) {
  const [quickText, setQuickText] = useState('');
  const [quickPreview, setQuickPreview] = useState<QuickCapturePreviewResponse | null>(null);
  const [quickForm, setQuickForm] = useState<MonthlyPlanItemCreatePayload | null>(null);
  const [quickError, setQuickError] = useState('');

  const clearPreview = () => {
    setQuickPreview(null);
    setQuickForm(null);
  };

  const clear = () => {
    setQuickText('');
    setQuickError('');
    clearPreview();
  };

  const previewMutation = useMutation({
    mutationFn: () =>
      previewMonthlyPlanQuickCapture(profileId, {
        rawText: quickText,
        defaultYear: year,
        defaultMonth: month,
        defaultCurrency: 'ARS',
      }),
    onSuccess: (response) => {
      setQuickError('');
      setQuickPreview(response);
      setQuickForm(response.parsed);
    },
    onError: (error) => {
      clearPreview();
      setQuickError(getApiErrorMessage(error));
    },
  });

  const commitMutation = useMutation({
    mutationFn: () => {
      if (!quickForm) {
        throw new Error('No hay previsualización confirmable.');
      }

      return commitMonthlyPlanQuickCapture(profileId, {
        rawText: quickText,
        payload: quickForm,
      });
    },
    onSuccess: () => {
      clear();
      invalidatePlanningViews();
    },
  });

  return {
    quickText,
    setQuickText,

    quickPreview,
    quickForm,
    setQuickForm,

    quickError,
    commitError: commitMutation.error ? getApiErrorMessage(commitMutation.error) : null,

    hasPreview: Boolean(quickPreview),
    canAnalyze: Boolean(quickText.trim()),

    analyze: () => previewMutation.mutate(),
    confirm: () => commitMutation.mutate(),
    discard: clearPreview,
    clear,

    isAnalyzing: previewMutation.isPending,
    isConfirming: commitMutation.isPending,
  };
}