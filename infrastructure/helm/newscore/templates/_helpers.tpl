{{/*
Common labels applied to every resource.
Usage: {{- include "newscore.labels" . | nindent 4 }}
*/}}
{{- define "newscore.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: newscore
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
newscore.de/environment: {{ .Values.global.environment }}
{{- end -}}

{{/*
Per-component name: <release>-<component>.
Usage: {{ include "newscore.componentName" (dict "root" $ "name" $name) }}
*/}}
{{- define "newscore.componentName" -}}
{{- printf "%s-%s" .root.Release.Name .name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Per-component selector labels.
Usage: {{- include "newscore.selectorLabels" (dict "root" $ "name" $name) | nindent 6 }}
*/}}
{{- define "newscore.selectorLabels" -}}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
{{- end -}}

{{/*
ServiceAccount name.
*/}}
{{- define "newscore.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (printf "%s-sa" .Release.Name) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}
