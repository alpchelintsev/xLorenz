import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class LorenzGUIClient extends JFrame implements ActionListener,ItemListener
{
	public static String getExtension(File f)
	{
		if(f != null)
		{
			String filename = f.getName();
			int i = filename.lastIndexOf('.');
			if(i > 0 && i < filename.length()-1)
				return filename.substring(i+1).toLowerCase();
		}
		return null;
	}

	class ExtFileFilter extends javax.swing.filechooser.FileFilter
	// Класс устраняет неудобства ввода фильтра в JFileChooser
	{
		String ext;
		String description;
		ExtFileFilter(String ext, String descr)
		{
			this.ext = ext;
			description = descr;
		}

		public boolean accept(File f)
		{
			if(f != null)
			{
				if(f.isDirectory())
					return true;
				String extension = getExtension(f);
				if(extension == null)
					return ext.length() == 0;
				return ext.equals(extension);
			}
			return false;
		}

		public String getExtension(File f)
		{
			return LorenzGUIClient.getExtension(f);
		}

		public String getDescription()
		{
			return description;
		}
	}

	String aboutProg = "О программе";
	String progName = "Построение проекции дуги траектории системы Лоренца";
	String authors = "\nCopyright © 2009 Пчелинцев А.Н.";
	String err_str = "Ошибка";

	void messageBox(String title, String text, int type_mes)
	{
		JOptionPane.showMessageDialog(this,text,title,type_mes);
	}

	CheckboxMenuItem p_mas[] = new CheckboxMenuItem[5];

	// Массив текстовых полей ввода
	JTextField tf[] = new JTextField[12];

	JLabel l_dt,cond,fpath;
	File cur_dir = null;

	// Кнопка "Вычислить"
	JButton calc;

	// Команды меню
	MenuItem p1,p2,p3,p4,p5;

	void setEnabled_delta_t(boolean flag)
	{
		l_dt.setEnabled(flag);
		tf[5].setEnabled(flag);
	}

	String getFilePath(JFileChooser ch, String ext)
	{
		File f = ch.getSelectedFile();
		String fp = f.getAbsolutePath();
		if(getExtension(f) == null)
			fp += ext;
		return fp;
	}

	void saveData(JFileChooser ch, boolean flag)
	{
		boolean f_fopen = false;
		BufferedWriter out = null; // Чтобы компилятор не ругался
		String fp = null;
		try
		{
			if(flag)
			{
				if(ch.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
					fp = getFilePath(ch, ".lor");
			}
			else
				fp = fpath.getText();
			if(fp != null)
			{
				out = new BufferedWriter(new FileWriter(fp));
				f_fopen = true;
				for(char i = 0; i < 17; i++)
					if(i < 12)
						out.write(tf[i].getText()+"\n");
					else
						if(p_mas[i-12].getState())
							out.write("1\n");
						else
							out.write("0\n");
				out.close();
				f_fopen = false;

				cur_dir = ch.getCurrentDirectory();
				fpath.setText(fp);
			}
		}
		catch(Exception ex)
		{
			if(f_fopen)
			try
			{
				out.close();
				(new File(fp)).delete();
			}
			catch(Exception _e)
			{
			}
			messageBox(err_str,"Ошибка записи в файл",
			                   JOptionPane.ERROR_MESSAGE);
		}
	}

	WindowAdapter wA;

	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == p1 || e.getSource() == p2 || e.getSource() == p3)
		{
			JFileChooser ch = new JFileChooser();
			ch.setFileFilter(new ExtFileFilter("lor",
			                                   "*.lor Файлы с параметрами программы"));
			if(cur_dir != null)
				ch.setCurrentDirectory(cur_dir);
			if(e.getSource() == p1) // Открыть
			{
				boolean f_fopen = false;
				BufferedReader in = null;
				if(ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
				try
				{
					String fp = ch.getSelectedFile().getAbsolutePath();
					in = new BufferedReader(new FileReader(fp));
					f_fopen = true;
					for(char i = 0; i < 17; i++)
						if(i < 12)
							tf[i].setText(in.readLine());
						else
							p_mas[i-12].setState(
							             in.readLine().equals("1"));
					setEnabled_delta_t(p_mas[3].getState());
					in.close();
					f_fopen = false;

					cur_dir = ch.getCurrentDirectory();
					fpath.setText(fp);
				}
				catch(Exception ex)
				{
					if(f_fopen)
					try
					{
						in.close();
					}
					catch(Exception _e)
					{
					}
					messageBox(err_str,"Ошибка чтения из файла",
					                   JOptionPane.ERROR_MESSAGE);
				}
			}
			else if(e.getSource() == p2) // Сохранить
				saveData(ch,fpath.getText().equals(" "));
			else // Сохранить как
				saveData(ch,true);
		}
		else if(e.getSource() == p4) // Выход
			wA.windowClosing(null);
		else // О программе
			messageBox(aboutProg,progName+authors,
			                     JOptionPane.INFORMATION_MESSAGE);
	}

	public void itemStateChanged(ItemEvent e)
	{
		if(e.getSource() == p_mas[0]) // xOy
		{
			p_mas[0].setState(true);
			p_mas[1].setState(false);
			p_mas[2].setState(false);
		}
		else if(e.getSource() == p_mas[1]) // xOz
		{
			p_mas[0].setState(false);
			p_mas[1].setState(true);
			p_mas[2].setState(false);
		}
		else if(e.getSource() == p_mas[2]) // yOz
		{
			p_mas[0].setState(false);
			p_mas[1].setState(false);
			p_mas[2].setState(true);
		}
		else if(e.getSource() == p_mas[3]) // Задается
			setEnabledComp(true);
		else // Вычисляется
			setEnabledComp(false);
	}

	void setEnabledComp(boolean flag)
	{
		p_mas[3].setState(flag);
		p_mas[4].setState(!flag);
		setEnabled_delta_t(flag);
	}

	void makeMenu()
	// Метод создания меню
	{
		// Создаем строку меню
		MenuBar myMenuBar = new MenuBar();
		setMenuBar(myMenuBar);

		// Определяем меню "Файл"
		Menu mfile = new Menu("Файл");

		// Создаем команды меню "Файл"
		p1 = new MenuItem("Открыть");
		p1.addActionListener(this);
		mfile.add(p1);

		p2 = new MenuItem("Сохранить");
		p2.addActionListener(this);
		mfile.add(p2);

		p3 = new MenuItem("Сохранить как");
		p3.addActionListener(this);
		mfile.add(p3);
		mfile.add(new MenuItem("-"));

		p4 = new MenuItem("Выход");
		p4.addActionListener(this);
		mfile.add(p4);
		// Добавляем в строку меню myMenuBar меню "Файл"
		myMenuBar.add(mfile);

		// Создаем меню "Тип проекции"
		Menu typeProj = new Menu("Тип проекции");

		// Создаем помечаемые пункты меню
		p_mas[0] = new CheckboxMenuItem("xOy",true);
		p_mas[0].addItemListener(this);
		typeProj.add(p_mas[0]);
		p_mas[1] = new CheckboxMenuItem("xOz");
		p_mas[1].addItemListener(this);
		typeProj.add(p_mas[1]);
		p_mas[2] = new CheckboxMenuItem("yOz");
		p_mas[2].addItemListener(this);
		typeProj.add(p_mas[2]);
		myMenuBar.add(typeProj);

		// Создаем меню "Шаг по времени"
		Menu deltaT = new Menu("Шаг по времени");
		p_mas[3] = new CheckboxMenuItem("Задается",true);
		p_mas[3].addItemListener(this);
		deltaT.add(p_mas[3]);
		p_mas[4] = new CheckboxMenuItem("Вычисляется");
		p_mas[4].addItemListener(this);
		deltaT.add(p_mas[4]);
		myMenuBar.add(deltaT);

		// Создаем меню "Справка"
		Menu mhelp = new Menu("Справка");
		p5 = new MenuItem(aboutProg);
		p5.addActionListener(this);
		mhelp.add(p5);
		myMenuBar.add(mhelp);
	}

	void makeComponents()
	// Метод создания компонентов в окне
	{
		setLayout(new BorderLayout());

		// Создаем панель с закладками
		JTabbedPane tabpane = new JTabbedPane();

		// Добавляем в нее панели
		tabpane.addTab("Параметры вычислений", new ParamCalcPane());
		tabpane.addTab("Расчет", new CalcPane());
		getContentPane().add(tabpane, BorderLayout.CENTER);

		JPanel p = new JPanel();
		fpath = new JLabel(" ");
		fpath.setFont(new Font(cond.getFont().getName(), Font.PLAIN, 10));
		p.add(fpath);
		add(p,BorderLayout.SOUTH);
	}

	class ParamCalcPane extends JPanel
	// Класс панели "Параметры вычислений"
	{
		ParamCalcPane()
		{
			setLayout(new GridLayout(9,2));
			JLabel lbl1 = new JLabel("\u03C3"); // Греческая буква sigma
			JLabel lbl2 = new JLabel("r");
			Font fnt = lbl2.getFont();
			lbl1.setFont(new Font("Symbol", fnt.getStyle(),
			             getFontMetrics(fnt).getHeight()));
			add(lbl1);
			tf[0] = new JTextField("10");
			add(tf[0]);

			add(lbl2);
			tf[1] = new JTextField("28");
			add(tf[1]);

			add(new JLabel("b"));
			tf[2] = new JTextField("2.66666666666");
			add(tf[2]);

			add(new JLabel("Длина отрезка времени"));
			tf[3] = new JTextField("6");
			add(tf[3]);

			add(new JLabel("Точность (степень числа 0.1)"));
			tf[4] = new JTextField("8");
			add(tf[4]);

			l_dt = new JLabel("Шаг по времени");
			add(l_dt);
			tf[5] = new JTextField("0.01");
			add(tf[5]);

			add(new JLabel("x0"));
			tf[6] = new JTextField("10");
			add(tf[6]);

			add(new JLabel("y0"));
			tf[7] = new JTextField("1");
			add(tf[7]);

			add(new JLabel("z0"));
			tf[8] = new JTextField("10");
			add(tf[8]);
		}
	}

	String gif_path;

	// Ресурсы, открываемые при передаче данных по сети
	Socket sock;
	BufferedWriter out_s;
	InputStream in_s;
	OutputStream out_f;

	// Флаги их открытия
	boolean f_sock = false, f_out_s = false, f_in_s = false, f_out_f = false;

	void freeResources()
	// Метод освобождает занятые ресурсы
	{
		try
		{
			if(f_out_f)
			{
				out_f.close();
				(new File(gif_path)).delete();
			}
			if(f_in_s)
				in_s.close();
			if(f_out_s)
				out_s.close();
			if(f_sock)
				sock.close();
		}
		catch(Exception e)
		{
		}
	}

	class ShowPicThread extends Thread
	// Класс, связанный с потоком просмотра полученного изображения
	{
		public void run()
		{
			try
			{
				Runtime.getRuntime().exec(tf[11].getText()+" "+gif_path);
			}
			catch(Exception e)
			{
			}
		}
	}

	ShowPicThread spTh;

	void create_spThread()
	{
		spTh = null; // Вызываем сборщик мусора
		spTh = new ShowPicThread();
		spTh.setPriority(Thread.NORM_PRIORITY);
	}

	class TransmitThread extends Thread
	// Класс, связанный с потоком передачи данных. Нужен для
	// того, чтобы при передаче данных не тормозил интерфейс
	{
		public void run()
		{
			calc.setEnabled(false);
			try
			{
				cond.setText("подключение к серверу");
				InetAddress addr = InetAddress.getByName(tf[9].getText());
				Integer port = Integer.valueOf(tf[10].getText());
				sock = new Socket(addr,port.intValue());
				f_sock = true;

				cond.setText("передача данных серверу");
				String ts = "";
				char i;
				for(i = 0; i < 9; i++)
					ts += tf[i].getText()+"\n";
				for(i = 0; i < 3; i++)
					if(p_mas[i].getState())
					{
						ts += Integer.toString(i)+"\n";
						break;
					}
				if(p_mas[3].getState())
					ts += "1";
				else
					ts += "0";
				out_s = new BufferedWriter(
				                    new OutputStreamWriter(sock.getOutputStream()));
				f_out_s = true;
				out_s.write(ts);
				out_s.close();
				f_out_s = false;

				cond.setText("идут вычисления");
				in_s = sock.getInputStream();
				f_in_s = true;
				out_f = new FileOutputStream(gif_path);
				f_out_f = true;

				boolean f_first = true;
				/*while(true)
				{*/
					int b = in_s.read();
					/*if(b == -1)
						break;*/
					out_f.write(b);

					if(f_first)
					{
						cond.setText("чтение данных по сети");
						f_first = false;
					}
				/*}*/

				// Закрываем потоки
				out_f.close();
				f_out_f = false;
				in_s.close();
				f_in_s = false;
				sock.close();
				f_sock = false;

				/*create_spThread();
				spTh.start();*/

				cond.setText("все OK; отключено");
			}
			catch(Exception e)
			{
				freeResources();
				cond.setText(e.getMessage());
			}
			calc.setEnabled(true);
		}
	}

	TransmitThread tTh;

	void create_tThread()
	{
		tTh = null;
		tTh = new TransmitThread();
		tTh.setPriority(Thread.NORM_PRIORITY);
	}

	void buttonClick()
	{
		JFileChooser ch = new JFileChooser();
		ch.setFileFilter(new ExtFileFilter("gif", "*.gif Графические файлы"));
		if(cur_dir != null)
			ch.setCurrentDirectory(cur_dir);
		if(ch.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			gif_path = getFilePath(ch, ".gif");
			create_tThread();
			tTh.start();
		}
	}

	class CalcPane extends JPanel
	// Класс панели "Расчет"
	{
		CalcPane()
		{
			setLayout(new BorderLayout());

			JPanel p = new JPanel();
			p.setLayout(new GridLayout(4,2));

			p.add(new JLabel("Host"));
			tf[9] = new JTextField("cluster.tstu.ru");
			p.add(tf[9]);

			p.add(new JLabel("Port"));
			tf[10] = new JTextField("2001");
			p.add(tf[10]);

			p.add(new JLabel("Просмотр gif-файла"));
			tf[11] = new JTextField();
			String OS = System.getProperty("os.name").toLowerCase();
			if(OS.indexOf("windows") > -1 || OS.indexOf("nt") > -1)
				tf[11].setText("mspaint.exe");
			else
				tf[11].setText("gimp");
			p.add(tf[11]);

			p.add(new JLabel("Состояние"));
			cond = new JLabel("отключено");
			p.add(cond);

			add(p,BorderLayout.CENTER);
			calc = new JButton("Вычислить");
			calc.addActionListener(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						buttonClick();
					}
				}
			);
			add(calc, BorderLayout.SOUTH);
		}
	}

	LorenzGUIClient()
	{
		setTitle(progName);
		BufferedImage image = null;
		try
		{
			image = ImageIO.read(getClass().getResource("icon.png"));
		}
		catch(Exception e)
		{
		}
		setIconImage(image);

		create_tThread();
		create_spThread();

		wA = new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				if(tTh.isAlive())
				{
					tTh.stop();
					try
					{
						tTh.join();
					}
					catch(Exception ex)
					{
					}
					freeResources();
				}
				if(spTh.isAlive())
				{
					spTh.stop();
					try
					{
						spTh.join();
					}
					catch(Exception ex)
					{
					}
				}
				System.exit(0);
			}
		};
		addWindowListener(wA);

		int w = 505;
		int h = 330;
		setSize(w,h);
		setResizable(false);

		// Центровка окна
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screen.width-w)/2, (screen.height-h)/2);

		makeMenu();
		makeComponents();
	}

	public static void main(String[] args)
	{
		LorenzGUIClient AppWindow = new LorenzGUIClient();
		AppWindow.show();
	}
}
