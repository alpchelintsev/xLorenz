#include <cstdio>
#include <cstdlib>
#include <cstring>

// Сетевые библиотеки
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>

// STL
#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>
using namespace std;
#include "count_lines.h"

// Для работы с клавиатурой
#include "kbhit.h"

// Размер буфера приема
#define BUF_SIZE 1024

int main(int argc, char *argv[])
{
	int num_port;

	if(argc!=2)
	{
		cout << endl << "Неправильный запуск сервера." << endl;
		return 1;
	}
	num_port = atoi(argv[1]); // Извлекаем из командной строки номер нода

	ifstream f("nodes.txt");
	if(!f)
	{
		cout << endl << "Ошибка при открытии файла nodes.txt." << endl;
		return 2;
	}

	stringstream ss;
	ss << f.rdbuf();
	f.close();

	int count_ports = count_lines(ss)-1;

	string s;
	ss >> s;

	// Определяем номер порта, на котором будет висеть текущий сервер
	num_port += atoi(s.c_str())+/*count_ports*/-1;

	// Создаем сокет
	int sock;
	sockaddr_in addr_node;
	if((sock = socket(PF_INET, SOCK_STREAM, 0)) < 0)
	{
		cout << endl << "Ошибка при создании сокета." << endl;
		return 3;
	}
	addr_node.sin_family = AF_INET;
	addr_node.sin_port = htons(num_port);
	addr_node.sin_addr.s_addr = INADDR_ANY;
	if(bind(sock, (sockaddr*)&addr_node, sizeof(addr_node)) < 0)
	{
		close(sock);
		cout << endl << "Ошибка присваивания сокету локального адреса." << endl;
		return 4;
	}

	init_keyboard();
	while(!kbhit())
	{
		if(listen(sock, 1) < 0) // Начинаем слушать
		{
			close_keyboard();
			close(sock);
			cout << endl << "Ошибка прослушивания порта." << endl;
			return 5;
		}
		int s;
		if((s = accept(sock, NULL, NULL)) < 0)
		{
			close_keyboard();
			close(sock);
			cout << endl << "Ошибка при принятии соединения." << endl;
			return 5;
		}

		ss.str("");
		// Принимаем данные от редиректора
		int bytes_read; // Количество прочитанных байт из сокета
		char buf[BUF_SIZE];
		memset(buf, '\0', BUF_SIZE);
		if(recv(s, buf, BUF_SIZE-1, 0) < 0)
		{
			close_keyboard();
			close(s);
			close(sock);
			cout << endl << "Ошибка при получении данных по сети." << endl;
			return 6;
		}
		ss << buf;

		// Производим необходимые вычисления
		cout << ss.str();

		FILE* f_gif = fopen("traj.gif", "rb");
		if(f_gif == NULL)
		{
			close_keyboard();
			close(s);
			close(sock);
			cout << endl << "Ошибка чтения файла traj.gif." << endl;
			return 7;
		}

		// Читаем результаты вычислений в память
		vector<char> gif_data;
		while(!feof(f_gif))
		{
			char b;
			// Читаем байт из файла traj.gif
			fread(&b, 1, 1, f_gif);
			gif_data.push_back(b);
		}
		fclose(f_gif);

		// Отсылаем результаты вычислений редиректору
		if(send(s, &gif_data[0], gif_data.size(), 0) < 0)
		{
			close_keyboard();
			close(s);
			close(sock);
			cout << endl << "Ошибка отправления данных по сети." << endl;
			return 8;
		}

		close(s);
	}
	close(sock);
	close_keyboard();

	return 0;
}
