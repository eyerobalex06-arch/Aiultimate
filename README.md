# AI OMNIVERSE

AI OMNIVERSE is an Android multi-provider AI chat app and model-family explorer.

## Download the APK

- [Latest installable APK](https://github.com/eyerobalex06-arch/Aiultimate/releases/tag/latest-build)
- [Build status and logs](https://github.com/eyerobalex06-arch/Aiultimate/actions/workflows/android.yml)

## What's included

- Native chat screen, rather than embedding only one hosted chat website
- AI Agent Group: create and customize role-based bots, run a shared group discussion where each agent can build on earlier replies, and queue missions using Android WorkManager so they can continue when the screen is closed (Android may defer background work)
- No app-imposed daily message quota; Auto-switch tries other configured providers when a provider fails, is rate-limited, or unavailable. Actual use remains subject to every provider’s quotas, account rules, model access, and capacity
- OpenRouter's `openrouter/free` model router, which can choose from its currently available free models
- Direct API connectors for Google Gemini, GroqCloud, Cerebras, Mistral Free Mode, and NVIDIA NIM, with editable model IDs. Defaults are intended for current model catalogs but provider availability and free access can change.
- Provider setup links and editable model IDs
- API keys encrypted locally with Android Keystore-backed AES-GCM encryption
- Provider credentials are not included in the app package; users supply their own keys
- Backups disabled so locally stored provider credentials are not included in Android app backup
- Searchable directory of model families and tasks

## Getting started

1. Open AI OMNIVERSE. The home screen now has **GET API KEY** and **ADD API KEY** actions above **OPEN MULTI-AI CHAT**.
2. Tap **GET API KEY**, choose a provider, and create your own key on that provider's official website. AI OMNIVERSE cannot generate third-party API keys on the provider's behalf.
3. Return to AI OMNIVERSE and tap **ADD API KEY**. The app opens the provider form with OpenRouter selected. Choose the provider you created a key for, paste the key, and press **SAVE KEY**.
4. The key is encrypted on this device. Select **Auto-switch** to let the app try your saved providers in order.
5. Send a message. The default OpenRouter model is `openrouter/free`, which selects from currently available free models.
6. For additional fallback options, configure Gemini, GroqCloud, Cerebras, Mistral Free Mode, and NVIDIA NIM using **GET API KEY** or **GET FREE KEY**. Only use free access unless you intentionally choose to pay.

Official account/key pages:
- OpenRouter: https://openrouter.ai/settings/keys
- Google AI Studio: https://aistudio.google.com/apikey
- GroqCloud: https://console.groq.com/keys
- Cerebras: https://cloud.cerebras.ai/platform
- Mistral: https://console.mistral.ai/api-keys/
- NVIDIA NIM: https://build.nvidia.com/

## AI Agent Group

- Add up to 8 named agents with your own role instructions, such as researcher, builder, critic, and coordinator. Type `@AgentName` in a mission to call out specific agents; otherwise the whole team participates.
- A mission runs agents in sequence. Each agent receives the original task and earlier group replies, making this a shared transcript rather than isolated parallel chats.
- Missions are queued as chained WorkManager jobs with network constraints and saved transcripts, so work can continue while the app is closed. Android may defer background execution, and provider rate limits can slow or stop a mission.
- The app does not autonomously browse the web, control your device, or complete external actions. Agents generate text responses based on their prompts and provider capabilities.

## Important limitations and cost safety

- This is a cloud-only app. No AI model runs locally on your phone, but chat requires internet access and sends prompts to a selected external provider.\n- No app can honestly promise infinite cloud AI usage. Free services impose request/token quotas, account rules, capacity limits, and changing model availability. Auto-switching helps use other configured services when possible; it does not bypass provider limits or guarantee that another provider will be available.
- The OpenRouter free-model router currently offers access to multiple free models, but its request limits and model pool can change. Check the provider's current plan and terms.
- Gemini, GroqCloud, and Cerebras need separate user-created API keys and each may limit use. You do not need to enter payment details unless the provider itself asks, and should not upgrade for this app.
- Model IDs change over time. If a provider rejects its default model ID, update that provider's **Model ID** field to a currently available model from the provider's official model list.
- Requests and chat content are sent to the provider that handles each message. Do not send passwords, private keys, or sensitive personal data. Read each provider's data policy. Provider data handling, uptime, quota and model access remain controlled by that provider.
- API keys are encrypted on this Android device with Android Keystore. They are not embedded in the APK or sent to AI OMNIVERSE servers. Anyone who obtains access to an unlocked device or a modified/rooted system may still pose a risk.
- The included model directory is informational; the app does not claim every model card is installed locally or individually connected.

## Build

The GitHub Actions workflow builds a debug APK using Gradle and Android SDK 35. A successful build proves compilation/package creation, not that every remote API works on every account/device. Real API requests require provider keys, internet access, and service availability.
