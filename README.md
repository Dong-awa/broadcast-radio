# Broadcast Radio Mod
A lightweight Minecraft Forge mod (1.20.1) that adds portable walkie-talkies for short-range text communication, perfect for survival multiplayer cooperation!

## 🎮 Features
- **Portable Walkie-Talkie**: Core handheld communication tool with 10 adjustable channels (1-10)
- **Short-Range Communication**: Messages are only visible to players within 128 blocks (balance for vanilla survival)
- **Power Simulation**: Uses durability as battery (100 total power, 1 power per channel switch, 1 power per message sent)
- **Frequency Switching**: Shift + Right Click to switch channels
- **Multiplayer Compatible**: Fully works in multiplayer servers, no extra server-side setup needed
- **Encrypted Communication**: Optional password protection (only visible to players with the same password) **(Not supported now)**
- **Vanilla-Friendly**: Fits perfectly into vanilla survival progression, no OP items/mechanics

## 📦 Installation
### Requirements
- Minecraft 1.20.1
- Forge 47.1.0+ (compatible with 1.20.1 Forge versions)

### Steps
1. Install Minecraft Forge for 1.20.1
2. Download the mod JAR file from [发布](https://github.com/Dong-awa/broadcast-radio/releases)
3. Put the JAR file into your `.minecraft/mods` folder
4. Launch the game with Forge profile

## 🕹️ Usage
1. Craft the Portable Walkie-Talkie
2. **Normal Right Click**: Trigger message input (type `your message` in chat to send)
3. Custom frequency from 1 to 999
4. Check remaining power by hovering over the walkie-talkie in inventory
5. Messages will appear in chat as `[Walkie-Talkie-X] Player: Message`

## 📝 Localization
Supports multiple languages:
- Simplified Chinese (zh_cn)
- Traditional Chinese (zh_tw, zh_hk)
- English (en_us)

## 🛠️ Development
### Build from Source
1. Clone this repository: `git clone https://github.com/Dong-awa/broadcast-radio.git`
2. Open the project in IntelliJ IDEA with Java 17
3. Run `gradlew build` (Windows) / `./gradlew build` (Mac/Linux)
4. The built JAR file will be in `build/libs/`

### Contributing
- Fork the repository
- Create a feature branch (`git checkout -b feature/AmazingFeature`)
- Commit your changes (`git commit -m 'Add some AmazingFeature'`)
- Push to the branch (`git push origin feature/AmazingFeature`)
- Open a Pull Request

## 📄 License
This mod is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Contact
- GitHub: [Dong-awa](https://github.com/Dong-awa)
- E-mail: 2099467463@qq.com
- Mod Repository: [https://github.com/Dong-awa/broadcast-radio](https://github.com/Dong-awa/broadcast-radio)
