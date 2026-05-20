# AI Reporter

A Burp Suite extension that uses Burp AI or local LLMs to automatically generate vulnerability reports from HTTP request/response pairs.

Right-click any request in Burp, select **Report with AI**, and the extension will produce a structured finding, creating a Burp Suite issue and optionally exporting it as a Markdown file ready for your pentest report.

It can be used with Burp Suite Professional and Burp Suite Community. In the latter one, Burp AI and issue reporting on Burp Suite cannot be used (Pro-only features), but it is still possible to generate issues using a local LLM (or a OpenAI compatible one) and report it in markdown format.

## Features

- **One-click reporting**: select a request, provide the vulnerability name, severity and confidence (plus optional additional details for issues that cannot be inferred from a single request, like an authorization bypass), and the LLM generates a complete finding
- **Dual LLM back-end**: switch between **Burp AI** and **Ollama / OpenAI compatible services** (via the standard `/v1/chat/completions` API) from the configuration tab
- **Optional markdown export**: findings can be saved also as structured Markdown files
- **Customisable prompts and templates**: edit the system prompts and templates, import/export them as text files, and restore defaults at any time
- **Interactive chat tab**: an interactive chat is supplied as a bonus, but it is a simple one. It saves history but it does not implements trimming or other advanced features

## Important note

If you choose Burp AI as LLM engine, this extension will consume AI Credits. At the moment, every user with a Burp Suite Professional license has 10,000 free credits, and once they are finished, additional credits need to be purchased. The extension’s credit consumption is usually moderate, but it also depends on the Burp AI backend infrastructure and on the size of the request/response being reported.

The same applies if a paid OpenAI compatible endpoint is used supplying an API Key.

Additionally, the reported requests and responses, along with the data entered in the popup created by the extension, are sent to PortSwigger’s AI infrastructure (if Burp AI is selected) or to the chosen OpenAI compatbile third party. Local Ollama model on the contrary **should not** disclose data to third parties.

## Requirements

- Burp Suite Professional or Community (Burp AI can be used only in the Pro version)
- Burp AI credits or a LLM endpoint (local Ollama instance or OpenAI compatible endpoint)

## Install

The last release can be downloaded in the Release section. Then load it in Burp via **Extensions → Add → Java → Select file**.

To accomplish with PortSwigger BAppStore policies, the extension requires the AI flag selected and AI feature enabled also for local models (for Pro users, on Burp Community the extension behaves differently). Main release has been developed this way and has been submitted to the BAppStore. I compiled a second version of the extension in the Release section without this check (a comment in the code shows where this check is), that you can use when you need to use local models without enabling Burp AI features (maybe for projects where it is mandatory to avoid sending data to third parties for contracts and policies).


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

## Usage

### Report a new vulnerability

1. In Burp's HTTP history, Repeater, or any message editor, right-click a request/response pair
2. Select **Report with AI**
3. Fill the **Vulnerability** field with the issue name and select **Severity** and **Confidence**
4. Optionally add **Additional details** on the issue (useful when you are reporting an issue that cannot be easily understood by the LLM from the single request/response pair, like an IDOR)
5. Click **OK** and the finding will be added as a Burp Suite issue. If Markdown export is enabled, it wiil also besaved to disk

**N.B.** You are using a LLM. It may happens, especially with small models, high temperatures, wrong prompts or big requests/responses, that LLM produce wrong issues or issues in a wrong format that are discarded by the extension. 

### Chat

Use the **AI Reporter** tab to have a free-form conversation with the LLM. The chat maintains conversation history for multi-turn interactions until you click **Clear History**.

**This is a very simple chat, only for quick requests or for debugging the extension. It saves history but it does not implements trimming or other advanced features. History is lost when Burp is closed or when it is cleared.**

## Prompts and templates customization

AI Reporter uses four customizable prompts and templates, editable from the **AI Reporter → Edit prompts and templates** menu.

| Name | Purpose |
|------|---------|
| `chatPrompt` | System prompt for the chat panel |
| `reportingPrompt` | System prompt used when generating an issue report |
| `userMessageTemplate` | User message sent to the LLM when reporting an issue |
| `markdownTemplate` | Format of the exported Markdown file |

Each prompt can be imported, exported and restored to its default value.

The extension expects LLM output in the following format, so keep it in your reporting prompt! 

```json
{
  "title": "Specific descriptive title of the finding",
  "details": "Detailed description including evidence, location, and impact",
  "remediation": "Specific and actionable remediation steps"
}
```

Sometimes Burp AI wrap output in Markdown also if not requested (JSON in a MD). For this reason, the extension removes markdown wrapping in the output.


### Template tags

`userMessageTemplate` and `markdownTemplate` support a set of **tags** that are replaced at runtime with data from the current HTTP request/response. Tags use the syntax `{{tag_name}}`.

#### Tags available in `userMessageTemplate`

| Tag | Replaced with |
|-----|--------------|
| `{{aireporter_issue_name}}` | The vulnerability name entered in the dialog |
| `{{aireporter_additional_details}}` | The additional details entered in the dialog (optional) |
| `{{aireporter_request}}` | Full HTTP request |
| `{{aireporter_response}}` | Full HTTP response |
| `{{aireporter_request_headers}}` | HTTP request headers only |
| `{{aireporter_response_headers}}` | HTTP response headers only |
| `{{aireporter_request_body}}` | HTTP request body |
| `{{aireporter_response_body}}` | HTTP response body |
| `{{aireporter_request_url}}` | Request URL |
| `{{aireporter_request_first_XX}}` | First `XX` bytes of the request |
| `{{aireporter_request_last_XX}}` | Last `XX` bytes of the request |
| `{{aireporter_response_first_XX}}` | First `XX` bytes of the response |
| `{{aireporter_response_last_XX}}` | Last `XX` bytes of the response |

**N.B.** Using only a portion of the request can be useful when requests/responses are very big or when the LLM you use have a limited context!

#### Tags available in `markdownTemplate`

| Tag | Replaced with |
|-----|--------------|
| `{{aireporter_title}}` | Issue title generated by the LLM |
| `{{aireporter_severity}}` | Severity selected in the dialog |
| `{{aireporter_confidence}}` | Confidence selected in the dialog |
| `{{aireporter_details}}` | Issue details generated by the LLM |
| `{{aireporter_remediation}}` | Remediation advice generated by the LLM |
| `{{aireporter_request}}` | Full HTTP request |
| `{{aireporter_response}}` | Full HTTP response |
| `{{aireporter_request_headers}}` | HTTP request headers only |
| `{{aireporter_response_headers}}` | HTTP response headers only |
| `{{aireporter_request_body}}` | HTTP request body |
| `{{aireporter_response_body}}` | HTTP response body |
| `{{aireporter_request_url}}` | Request URL |
| `{{aireporter_request_first_XX}}` | First `XX` bytes of the request |
| `{{aireporter_request_last_XX}}` | Last `XX` bytes of the request |
| `{{aireporter_response_first_XX}}` | First `XX` bytes of the response |
| `{{aireporter_response_last_XX}}` | Last `XX` bytes of the response |

## License

MIT License

Copyright (c) 2026 federicodotta

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
