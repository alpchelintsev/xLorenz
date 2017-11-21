#include <termios.h>
#include <unistd.h> // read()

struct termios old_settings, new_settings;
int peek_character;

void init_keyboard()
{
	tcgetattr(0, &old_settings);

	new_settings = old_settings;
	new_settings.c_lflag &= ~ICANON;
	new_settings.c_lflag &= ~ECHO;
	new_settings.c_lflag &= ~ISIG;
	new_settings.c_cc[VMIN] = 1;
	new_settings.c_cc[VTIME] = 0;
	tcsetattr(0, TCSANOW, &new_settings);

	peek_character=-1;
}

void close_keyboard()
{
	tcsetattr(0, TCSANOW, &old_settings);
}

int kbhit()
{
	unsigned char ch;
	int nread;

	if(peek_character != -1)
		return 1;

	new_settings.c_cc[VMIN] = 0;
	tcsetattr(0, TCSANOW, &new_settings);
	nread = read(0, &ch, 1);
	new_settings.c_cc[VMIN] = 1;
	tcsetattr(0, TCSANOW, &new_settings);

	if(nread == 1)
	{
		peek_character = ch;
		return 1;
	}
	return 0;
}

int getch()
{
	char ch;

	if(peek_character != -1)
	{
		ch = peek_character;
		peek_character = -1;
	}
	else read(0, &ch, 1);

	return ch;
}
