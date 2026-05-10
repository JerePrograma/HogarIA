import { jsx as _jsx } from "react/jsx-runtime";
const toneClass = { neutral: 'badge', ok: 'badge badge-ok', watch: 'badge badge-watch', risk: 'badge badge-risk', critical: 'badge badge-critical' };
export function StatusBadge({ label, tone = 'neutral' }) { return _jsx("span", { className: toneClass[tone], children: label }); }
