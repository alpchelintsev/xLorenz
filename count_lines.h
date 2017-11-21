int count_lines(stringstream &ss)
{
	string buf = ss.str();
	int c = 1;
	int l = buf.length();
	for(int i = 0; i < l; i++)
		if(buf[i] == '\n')
			c++;
	return c;
}
