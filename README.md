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


### Per-model API key storage and chat navigation
- API keys are encrypted on-device and stored separately for each provider + model ID pair. Saving a key for one model does not overwrite another model's key. The chat toolbar's three-dot menu includes add/change, official provider key pages, delete current model key, and jump-to-top/bottom shortcuts.
- Use the TOP and BOTTOM controls beneath the conversation to scroll through chat history. The model search field is now nearer the top of the home screen.


## Chat interface and saved conversations

- The chat screen uses a conversation-first layout with a compact model picker, starter prompts, a bottom message composer that respects Android navigation insets, and a new-chat button.
- Choose Auto-switch or select a preferred provider/model from OpenRouter, Google Gemini, GroqCloud, Cerebras, Mistral AI, or NVIDIA NIM. Custom model IDs can be entered under **Manage API keys / providers**.
- Up to 30 conversations are kept locally on the phone and saved as you send and receive messages. Open the three-dot menu and choose **Your conversations** to return to an earlier conversation. Starting a new chat does not erase the previous one.
- To create an automatic cloud-backup file, open the three-dot menu, choose **Set up / change OneDrive backup**, and select a writable OneDrive location in the Android file picker. AI OMNIVERSE will update the selected JSON file when conversations change and when the chat screen is left. Use **Restore conversations from backup** to import a backup later.
- Availability of OneDrive as a writable destination depends on the OneDrive app and Android's document-provider picker on the device. If OneDrive is not offered there, this file-picker backup route cannot write directly to it; a Microsoft Graph / OneDrive app registration would be needed for direct account-based sync. Local conversation saving works regardless.
- The conversation backup contains chat text and metadata. Do not place sensitive information in conversations or backups unless you are comfortable storing it in that destination.
