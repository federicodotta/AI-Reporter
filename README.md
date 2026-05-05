# AI Reporter

A Burp Suite extension that uses Burp AI or local LLMs to automatically generate vulnerability reports from HTTP request/response pairs.

Right-click any request in Burp, select **Report with AI**, and the extension will produce a structured finding, creating a Burp Suite issue and optionally exporting it as a Markdown file ready for your pentest report.

## Features

- **One-click reporting**: select a request, provide the vulnerability name, severity and confidence (plus optional additional details for issues that cannot be inferred from a single request, like an authorization bypass), and the LLM generates a complete finding
- **Dual LLM back-end**: switch between **Burp AI** and **Ollama / OpenAI** (via the standard `/v1/chat/completions` API) from the configuration tab
- **Optional markdown export**: findings can be saved also as structured Markdown files
- **Customisable prompts**: edit the system prompts, import/export them as text files, and restore defaults at any time
- **Interactive chat tab**: an interactive chat is supplied as a bonus, but it is a simple one. It saves history but it does not implements trimming or other advanced features

## Important note

If you choose Burp AI as LLM engine, this extension will consume AI Credits. At the moment, every user with a Burp Suite Professional license has 10,000 free credits, and once they are finished, additional credits need to be purchased. The extension’s credit consumption is usually moderate, but it also depends on the Burp AI backend infrastructure and on the size of the request/response being reported.

The same applies if a paid OpenAI compatible endpoint is used supplying an API Key.

Additionally, the reported requests and responses, along with the data entered in the popup created by the extension, are sent to PortSwigger’s AI infrastructure (if Burp AI is selected) or to the chosen OpenAI compatbile third party. Local Ollama model on the contrary should not disclose data to third parties.

## Requirements

- Burp Suite Professional or Community (Burp AI can be used only in the Pro version)
- Burp AI credits or a LLM endpoint (local Ollama instance or OpenAI compatible endpoint)

## Install

The last release can be downloaded in the Release section. Then load it in Burp via **Extensions → Add → Java → Select file**.

The extension will be submitted also on Burp Suite BAppStore. Stay tuned!


## Build

```bash
git clone https://github.com/federicodotta/AiReporter.git
cd AiReporter
gradlew jar
```

The fat JAR is generated in `build/libs/`. 

## Configuration

The following configuiration can be set the **AI Reporter** tab in Burp Suite (Burp AI requires less configurations):

| Field | Burp AI | Ollama/OpenAI | Description | 
|---|:---:|:---:|---|
| **LLM Provider** | ✅ | ✅ | `Burp AI` or `OpenAI / Ollama` |
| **Endpoint URL** | ❌ | ✅ | Not necessary for Burp AI, base URL of the LLM API (e.g. `http://localhost:11434` for Ollama, `https://api.openai.com` for OpenAI) |
| **Model** | ❌ | ✅ | Model identifier (es. `gpt-oss:20b`) |
| **API Key** | ❌ | ✅ | API key for authenticated providers (leave empty for local Ollama) |
| **Temperature** | ✅ | ✅ | LLM temperature (0.0 to call LLM without supplying the temperature, using the provider default) |
| **HTML Encoding** | ✅ | ✅ | `YES` to HTML-encode LLM output before using it for details and remediation in Burp Suite issues |

Click **Apply** to save. Settings persist across Burp restarts.

**AI Reporter** menu entry allows to edit prompts and change markdown exporting options.

## Usage

### Report a new vulnerability

1. In Burp's HTTP history, Repeater, or any message editor, right-click a request/response pair
2. Select **Report with AI**
3. Fill the **Vulnerability** field with the issue name and select **Severity** and **Confidence**
4. Optionally add **Additional details** on the issue (useful when you are reporting an issue that cannot be easily understood by the LLM from the single request/response pair, like an IDOR)
5. Click **OK** and the finding will be added as a Burp Suite issue. If Markdown export is enabled, it wiil also besaved to disk

### Chat

Use the **AI Reporter** tab to have a free-form conversation with the LLM. The chat maintains conversation history for multi-turn interactions until you click **Clear History**.

**This is a very simple chat, only for quick requests or for debugging the extension. It saves history but it does not implements trimming or other advanced features. History is lost when Burp is closed or when it is cleared.**

### Prompt customisation

From the menu bar: **AI Reporter → Edit prompts**

Two prompts are available:

- **reportPrompt**: system prompt for the reporting task
- **chatPrompt**: system prompt for the interactive chat tab

Each tab supports import/export and restore to default.

## License

TODO
