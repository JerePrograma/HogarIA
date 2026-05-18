import { useQuickCaptureFlow } from '../../../../hooks/useQuickCaptureFlow';
import { QuickCapturePanel } from '../../components/QuickCapturePanel';
import { QuickCapturePreviewForm } from '../../components/QuickCapturePreviewForm';
import { usePlanningData } from '../../hooks/usePlanningData';

export function MonthlyPlanImportPage(){ const { profileId,year,month,accounts,categories,invalidatePlanningViews }=usePlanningData(); const quick=useQuickCaptureFlow({profileId,year,month,invalidatePlanningViews}); return <QuickCapturePanel input={quick.quickText} onChange={quick.setQuickText} onAnalyze={quick.analyze} canAnalyze={quick.canAnalyze} error={quick.quickError} showDiscard={quick.hasPreview} onDiscard={quick.discard} onClear={quick.clear} isAnalyzing={quick.isAnalyzing}>{quick.quickPreview && quick.quickForm ? <QuickCapturePreviewForm profileId={profileId} preview={quick.quickPreview} form={quick.quickForm} setForm={quick.setQuickForm} accounts={accounts} categories={categories} onConfirm={quick.confirm} isConfirming={quick.isConfirming} error={quick.commitError}/> : null}</QuickCapturePanel>; }
