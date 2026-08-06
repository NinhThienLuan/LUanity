# LUanity — AI Game Translation Proxy

A real-time AI-powered translation proxy and desktop dashboard for Unity games. Bridges XUnity.AutoTranslator (BepInEx) to local or cloud LLMs — enabling offline, context-aware, glossary-preserving game translation with a single click.

---

## Prerequisites

Before you begin, ensure you meet the following requirements:

- **Java 17+** (JDK 21 recommended for JavaFX 21 compatibility)
- **Apache Maven 3.8+**
- **A Unity game** with BepInEx support (Mono or IL2CPP)
- *(Optional)* [Ollama](https://ollama.com) running locally with a model such as `qwen2.5:3b` or `gemma2:2b`
- *(Optional)* A Gemini or OpenAI API key for cloud-based translation

---

## Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/NinhThienLuan/tool_dich_game.git
   cd tool_dich_game
   ```

2. **Build the project**
   ```bash
   mvn clean package -DskipTests
   ```

3. **Run the Desktop Dashboard**
   ```bash
   mvn spring-boot:run
   ```

4. *(Optional)* **Run in headless CLI mode** (no UI, background service only)
   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments="--cli"
   ```

---

## Usage

### 1. Add a Game

- Open the Dashboard → **"Chọn Game"** dropdown.
- Browse and select your game's `.exe` file.
- LUanity automatically detects Unity architecture (Mono or IL2CPP).

### 2. Install BepInEx + AutoTranslator (One Click)

- Click **"Cài đặt BepInEx"** in the Actions panel.
- The tool automatically downloads the correct BepInEx release and XUnity.AutoTranslator for your game, then writes a pre-configured `AutoTranslatorConfig.ini` pointing to the local proxy.

### 3. Choose Translation Provider

| Provider | Mode | Notes |
| :--- | :--- | :--- |
| `qwen2.5:3b` / `gemma2:2b` | Local (Ollama) | Offline, fast, no API key needed |
| Google Translate V2 | Cloud (free tier) | Fast, no context-awareness |
| Gemini Flash | Cloud (API key) | Best quality, context-aware |
| OpenAI GPT | Cloud (API key) | High quality, higher cost **(not supported yet)** |

### 4. Configure Glossary Presets

Select thematic presets (e.g., Wuxia, Dark Fantasy, Sci-Fi) from the Preset dropdown to enforce consistent character name and terminology translation across the entire game session.

### 5. Start the Game

Launch the game normally. XUnity.AutoTranslator routes all in-game text through this proxy automatically.

---

## Project Structure

```
├── src/main/java/com/aiwrapper/
│   ├── config/          # App configuration and AI provider settings
│   ├── executor/        # Translation executor & batch queue pipeline
│   ├── file/            # File handler strategies
│   ├── javafx/ui/       # Desktop UI controllers
│   ├── provider/        # AI providers (Ollama, Gemini, OpenAI, Google)
│   └── template/        # Prompt renderer templates
├── data/
│   ├── presets/         # Thematic glossary JSON files
│   ├── game_history.json
│   └── app_config.json
```

---

## Contributing

Contributions are welcome! To contribute:

1. Fork this repository.
2. Create a branch: `git checkout -b feature/your-feature`.
3. Commit your changes: `git commit -m 'Add some feature'`.
4. Push and open a Pull Request.

Please make sure all existing tests pass before submitting:
```bash
mvn test
```

---

## Credits & Acknowledgements

This project is built on top of the following open-source projects:

- **[BepInEx](https://github.com/BepInEx/BepInEx)** — Unity mod framework. Licensed under [LGPL-2.1](https://github.com/BepInEx/BepInEx/blob/master/LICENSE).
- **[XUnity.AutoTranslator](https://github.com/bbepis/XUnity.AutoTranslator)** — In-game automatic text translation hook. Licensed under [MIT](https://github.com/bbepis/XUnity.AutoTranslator/blob/master/LICENSE).

---

## License

This project is licensed under the **MIT License**.

```
MIT License

Copyright (c) 2026 NinhThienLuan

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
```
