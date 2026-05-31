#pragma once

#include <list>

class Chat : public ListBox
{
public:
	Chat();

	void addChatMessage(const std::string& message, const std::string& nick, const ImColor& nick_color);
	void addInfoMessage(const std::string& format, ...);
	void addDebugMessage(const std::string& format, ...);
	void addClientMessage(const std::string& message, const ImColor& color);

	virtual void keyboardEvent(const std::string& input) override;

private:
	void addMessage(const std::string& messsage, const ImColor& color = ImColor(1.0f, 1.0f, 1.0f));
	void addPlayerMessage(const std::string& message, const std::string& nick, const ImColor& nick_color);
};
