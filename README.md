# AI OMNIVERSE

AI OMNIVERSE is an Android app for exploring AI model families and opening a live hosted AI chat experience from one interface.

## Download the APK

The build workflow creates a debug APK and publishes it here after a successful build:

- [Latest APK release](https://github.com/eyerobalex06-arch/Aiultimate/releases/tag/latest-build)
- [Build status and logs](https://github.com/eyerobalex06-arch/Aiultimate/actions/workflows/android.yml)

## Features

- Dark, purple, mobile-first model explorer
- Search model families, strengths, tasks, and modality
- Filters for Coding, Reasoning, Fast, Images, Audio, Local, and Research
- Individual model cards explaining likely strengths and trade-offs
- In-app Live AI Chat screen powered by the hosted Hugging Face Chat website
- Remembers web chat cookies as supported by Android WebView
- Reopen/retry the hosted chat when the network is available

## Important boundaries

The catalog's model cards are a guide to model families. They do not mean every listed model is installed locally or directly connected to this app. The Live AI Chat screen opens Hugging Face's hosted chat service inside the app; Hugging Face account requirements, model availability, rate limits, and terms are controlled by that service. This is not an offline multi-model engine, and it does not contain private provider API keys.

Strength labels are practical guidance, not a universal benchmark ranking. Specific strengths vary by model version and task.
