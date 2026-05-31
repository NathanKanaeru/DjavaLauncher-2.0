#include "../gui.h"
#include "../../main.h"
#include "../../game/game.h"
#include "../../net/netgame.h"
#include <algorithm>
#include "../settings.h"
#include "java/jniutil.h"

extern UI* pUI;
extern CGame* pGame;
extern CNetGame* pNetGame;
extern CSettings* pSettings;
extern CJavaWrapper *pJavaWrapper;

Chat::Chat()
	: ListBox()
{

}

void Chat::addChatMessage(const std::string& message, const std::string& nick, const ImColor& nick_color)
{
	addPlayerMessage(message, nick, nick_color);
}

void Chat::addInfoMessage(const std::string& format, ...)
{
	va_list args;
	va_start(args, format);

	size_t length = vsnprintf(nullptr, 0, format.c_str(), args) + 1;
	va_end(args);

	std::string formattedString(length, '\0');
	va_start(args, format);
	vsnprintf(&formattedString[0], length, format.c_str(), args);
	va_end(args);

	addMessage(formattedString, ImColor(0x00, 0xc8, 0xc8));
}

void Chat::addDebugMessage(const std::string& format, ...)
{
	va_list args;
	va_start(args, format);

	size_t length = vsnprintf(nullptr, 0, format.c_str(), args) + 1;
	va_end(args);

	std::string formattedString(length, '\0');
	va_start(args, format);
	vsnprintf(&formattedString[0], length, format.c_str(), args);
	va_end(args);

	addMessage(formattedString, ImColor(0xbe, 0xbe, 0xbe));
}

void Chat::addClientMessage(const std::string& message, const ImColor& color)
{
	addMessage(message, color);
}

void Chat::addMessage(const std::string& message, const ImColor& color)
{
	if (pJavaWrapper)
	{
		char colorStr[16];
		sprintf(colorStr, "{%02X%02X%02X}",
			(int)(color.Value.x * 255),
			(int)(color.Value.y * 255),
			(int)(color.Value.z * 255));
		std::string coloredMsg = colorStr + message;
		pJavaWrapper->AddChatMessage(coloredMsg.c_str());
	}
}

void Chat::addPlayerMessage(const std::string& message, const std::string& nick, const ImColor& nick_color)
{
	if (pJavaWrapper)
	{
		char nickColorStr[16];
		sprintf(nickColorStr, "{%02X%02X%02X}",
			(int)(nick_color.Value.x * 255),
			(int)(nick_color.Value.y * 255),
			(int)(nick_color.Value.z * 255));
		std::string formatted = nickColorStr + nick + "{FFFFFF}: " + message;
		pJavaWrapper->AddChatMessage(formatted.c_str());
	}
}

void Chat::keyboardEvent(const std::string& input)
{
	if (input.length() > 0 && pNetGame)
	{
		if (input[0] == '/') pNetGame->SendChatCommand(input.c_str());
		else pNetGame->SendChatMessage(input.c_str());
	}
}
