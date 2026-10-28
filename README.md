# AI OMNIVERSE

AI OMNIVERSE is an Android multi-provider AI chat app and model-family explorer.

## Download the APK

- [Latest installable APK](https://github.com/eyerobalex06-arch/Aiultimate/releases/tag/latest-build)
- [Build status and logs](https://github.com/eyerobalex06-arch/Aiultimate/actions/workflows/android.yml)

## What's new in v2

- Native chat screen, rather than embedding only one hosted chat website
- Auto-switch router: when a configured provider fails, is rate-limited, or is unavailable, the app tries another configured provider
- OpenRouter's `openrouter/free` model router, which can choose from its currently available free models
- Direct API connectors for Google Gemini, GroqCloud, and Cerebras
- Provider setup links and editable model IDs
- API keys encrypted locally with Android Keystore-backed AES-GCM encryption
- Provider credentials are not included in the app package; users supply their own keys
- Backups disabled so locally stored provider credentials are not included in Android app backup
- Searchable directory of model families and tasks

## Getting started

1. Install AI OMNIVERSE on Android and open **OPEN MULTI-AI CHAT**.
2. Select **OpenRouter Free Models**, then tap **GET FREE KEY** to create an OpenRouter API key.
3. Return to the app, paste the key and press **SAVE KEY**. The app encrypts the key on this device.
4. Send a message. The default OpenRouter model is `openrouter/free`, which selects from currently available free models.
5. For more fallback options, repeat the process for Gemini, GroqCloud, and Cerebras. Then select **Auto-switch** so the app can try every configured provider if one fails.

Official account/key pages:
- OpenRouter: https://openrouter.ai/settings/keys
- Google AI Studio: https://aistudio.google.com/apikey
- GroqCloud: https://console.groq.com/keys
- Cerebras: https://cloud.cerebras.ai/platform

## Important limitations

- No app can honestly promise infinite cloud AI usage. Free services impose request/token quotas, account rules, capacity limits, and changing model availability. Auto-switching helps use other configured services when possible; it does not bypass provider limits.
- The OpenRouter free-model router currently offers access to multiple free models, but its request limits and model pool can change. Check the provider's current plan and terms.
- Gemini, GroqCloud, and Cerebras need separate user-created API keys and each may limit use. You do not need to enter payment details unless the provider itself asks, and should not upgrade for this app.
- Model IDs change over time. If a provider rejects its default model ID, update that provider's **Model ID** field to a currently available model from the provider's official model list.
- Requests and chat content are sent to the provider that handles each message. Do not send passwords, private keys, or sensitive personal data. Read each provider's data policy. Provider data handling, uptime, quota and model access remain controlled by that provider.
- API keys are encrypted on this Android device with Android Keystore. They are not embedded in the APK or sent to AI OMNIVERSE servers. Anyone who obtains access to an unlocked device or a modified/rooted system may still pose a risk.
- The included model directory is informational; the app does not claim every model card is installed locally or individually connected.

## Build

The GitHub Actions workflow builds a debug APK using Gradle and Android SDK 35. A successful build proves compilation/package creation, not that every remote API works on every account/device. Real API requests require provider keys, internet access, and service availability.
