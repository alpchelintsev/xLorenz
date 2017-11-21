#include <cstdlib>

// Библиотеки системных функций Linux
#include <pthread.h>
#include "kbhit.h"

// Сетевые библиотеки
#include <netdb.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
using namespace std;
#include "count_lines.h"

// Размер буфера приема
#define BUF_SIZE 1024
#define IF_CLOSE_SOCKETS if(ob.flag_sock)\
                         {\
                               close(s);\
                               close(ob.sock_gui_client);\
                               close(ob.sock_node_server);\
                               break;\
                         }

class ThreadForNode
// Класс, связанный с потоком, ослуживающим нод кластера
{
public:
	char flag_thread; // Флаг создания потока
	pthread_t th;     // Идентификатор потока

	char flag_sock;       // Флаг удачной работы с сокетом
	int sock_gui_client;  // Дескриптор сокета для GUI-клиента
	int sock_node_server; // Дескриптор сокета для сервера на ноде

	static void* th_function(void* p)
	// Потоковая функция. Этот метод должен быть static
	// или глобальным, чтобы туда неявно this не передовался
	{
		ThreadForNode ob = *(ThreadForNode*)p;
		int s;          // Дескриптор сокета
		int bytes_read; // Количество прочитанных байт из сокета
		char buf[BUF_SIZE];
		if(!ob.flag_sock)
			while(1)
			{
				ob.flag_sock = listen(ob.sock_gui_client, 1) < 0;
				if(ob.flag_sock)
					break;
				s = accept(ob.sock_gui_client, NULL, NULL);
				if(s < 0)
					continue;

				// Читаем данные от GUI-клиента и передаем их ноду кластера
				if((bytes_read = recv(s, buf, BUF_SIZE, 0)) < 0)
					break;
				if(send(ob.sock_node_server, buf, bytes_read, 0) < 0)
					break;

				// Читаем результат (gif-картинка) вычислений
				char flag_size = 1;
				int sz, t_sz = 0;
				while(1)
				{
					ob.flag_sock = (bytes_read = recv(ob.sock_node_server,
				        	                          buf, BUF_SIZE, 0)) < 0;
					IF_CLOSE_SOCKETS
					cout << "11";
					if(flag_size)
					{
						sz = *(int*)buf;
						cout << sz;
						flag_size = 0;
					}
					else
					{
						t_sz += bytes_read;
						if(t_sz == sz)
							break;
						ob.flag_sock = send(s, buf, bytes_read, 0) < 0;
						IF_CLOSE_SOCKETS
					}
				}
				if(ob.flag_sock)
					break;

				close(s);
			}
		ob.flag_thread = 0;
	}
};

class RedirectorNode: private ThreadForNode
{
	sockaddr_in addr_director; // Структура для получения IP-адреса
	sockaddr_in addr_node;
public:
	// Конструктор создает необходимые сокеты
	RedirectorNode(string ip_node, int port_gui_client, int port_dislocat)
	{
		// Создаем сокет для GUI-клиента
		flag_sock = (sock_gui_client = socket(PF_INET, SOCK_STREAM, 0)) < 0;
		if(flag_sock)
			return;
		addr_director.sin_family = AF_INET;
		addr_director.sin_port = htons(port_gui_client);
		addr_director.sin_addr.s_addr = INADDR_ANY;
		flag_sock = bind(sock_gui_client, (sockaddr*)&addr_director,
		                                             sizeof(addr_director)) < 0;
		if(flag_sock)
			return;

		// Создаем сокет для сервера, находящегося на ноде кластера
		flag_sock = (sock_node_server = socket(PF_INET, SOCK_STREAM, 0)) < 0;
		if(flag_sock)
		{
			close(sock_gui_client);
			return;
		}

		addr_node.sin_family = AF_INET;
		addr_node.sin_port = htons(port_gui_client+port_dislocat);
		hostent* host = gethostbyname(ip_node.c_str());
		flag_sock = host == NULL;
		if(flag_sock)
		{
			close(sock_gui_client);
			close(sock_node_server);
			return;
		}
		addr_director.sin_addr.s_addr = inet_addr(*host->h_addr_list);
		flag_sock = connect(sock_node_server, (sockaddr*)&addr_node, sizeof(addr_node)) < 0;
		if(flag_sock)
		{
			close(sock_gui_client);
			close(sock_node_server);
		}
	}

	// Освобождение занятых ресурсов
	~RedirectorNode()
	{
		if(flag_thread)
		{
			// Завершаем поток
			pthread_cancel(th);
			// Блокируем вызывающий поток до завершения потока th
			pthread_join(th, NULL);
		}
		if(!flag_sock)
		{
			close(sock_gui_client);
			close(sock_node_server);
		}
	}

	// Перегрузка оператора ! для проверки флага открытия сокетов
	char operator!()
	{
		return flag_sock;
	}

	// Метод создает поток
	void CreateThread()
	{
		flag_thread = !pthread_create(&th, NULL, th_function, (ThreadForNode*)this);
		// NULL - атрибуты потока по умолчанию
		// this - аргумент потоковой функции
	}
};

int main()
{
	ifstream f("nodes.txt");
	if(!f)
	{
		cout << endl << "Ошибка при открытии файла nodes.txt." << endl;
		return 1;
	}

	stringstream ss;
	ss << f.rdbuf();
	f.close();

	int count_ports = count_lines(ss)-1;
	RedirectorNode** nodes = new RedirectorNode*[count_ports];

	string s;
	ss >> s;
	int base_port = atoi(s.c_str());

	int n;
	for(n = 0; n < count_ports; n++)
	{
		ss >> s;
		nodes[n] = new RedirectorNode(s, base_port+n, count_ports);
		nodes[n] -> CreateThread();
	}

	init_keyboard();
	while(!kbhit())
	{
		int flag = 1;
		for(n = 0; n < count_ports; n++)
			if(!!*nodes[n])
			{
				flag = 0;
				break;
			}
		if(flag)
			break;
	}
	close_keyboard();

	for(n = 0; n < count_ports; n++)
		delete nodes[n];

	return 0;
}
