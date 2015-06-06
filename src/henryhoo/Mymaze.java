/**
 * 
 */
package henryhoo;

import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.APDU;
import javacard.framework.SystemException;
import javacard.framework.Util;

/**
 * @author henry
 * 
 */
public class Mymaze extends Applet {
	final static byte SET_MAP = (byte) 0x82; // 1�������Թ�
	final static byte GET_ROUTE = (byte) 0x71; // 2����ÿ���·��
	final static byte GET_MAXWEIGHT = (byte) 0x72; // 3��������Ȩֵ·��
	final static byte GET_MINWEIGHT = (byte) 0x73; // 4�������С��ֵ����·����ʼ����ֵ
	final static byte GET_SNUMBER = (byte) 0x80; // 5������ѧ��2011210986
	final static byte GET_SNAME = (byte) 0x81; // 6����������gb2312����45854893

	final static short SW_FINDROUTE_FAILED = 0x6A01; // �����޷��߳��Թ�
	final static short SW_WRONG_WEIGHT = 0x6A02; // ����ֵ��Ȩֵ����Ҫ��δ��-10~10��
	final static short SW_NO_RESULT = 0x6A03;// ���·���޽��
	final static short SW_WRONG_LC = 0x6A04; // ����LC
	final static short SW_WRONG_MN = 0x6A05;// �������

	private byte snumber[] = { (byte) 0x32, (byte) 0x30, (byte) 0x31,
			(byte) 0x32, (byte) 0x32, (byte) 0x31, (byte) 0x31, (byte) 0x30,
			(byte) 0x30, (byte) 0x39 }; // ѧ��
	private byte sname[] = { (byte) 0x26, (byte) 0x46, (byte) 0x28,
			(byte) 0x49, (byte) 0x84, (byte) 0x43 }; // ����

	byte m; // �Թ�����
	byte n; // �Թ�����
	short length; // �����ܳ���
	byte[] power; // ÿ����Ȩֵ
	short[] hp; // ��������ֵ
	byte[] path; // ��¼·��\
	byte[] flag;
	Node[] node;

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new Mymaze().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		if ((buffer[ISO7816.OFFSET_CLA]) == 0
				&& (buffer[ISO7816.OFFSET_INS]) == (byte) (0xA4))
			return;
		switch (buffer[ISO7816.OFFSET_INS]) {
		case GET_SNUMBER:
			Util.arrayCopyNonAtomic(snumber, (short) 0, buffer, (short) 0,
					(short) 10);
			apdu.setOutgoingAndSend((short) 0, (short) 10);
			break;
		case GET_SNAME:
			Util.arrayCopyNonAtomic(sname, (short) 0, buffer, (short) 0,
					(short) 6);
			apdu.setOutgoingAndSend((short) 0, (short) 6);
			break;
		case SET_MAP:
			map(apdu);
			break;
		case GET_ROUTE:
			if (getroutelong()) {
				printroute(path, hp[n * m - 1], apdu);
			} else {
				ISOException.throwIt(SW_FINDROUTE_FAILED);// �����·��
			}
			break;
		case GET_MAXWEIGHT:
			getmaxweight();// Dijkstra�㷨˼�룬�����ʼ�㣨0��0����ÿ���ڵ�����·�������ֵ
			if (node[m * n - 1].high > 0) {
				for (short i = 0; i < length; i++) {
					if (node[(short) (node[m * n - 1].hpath[i] & 0xff)].high < 0) {
						ISOException.throwIt(SW_FINDROUTE_FAILED);// �����·��
					}
				}
				printroute(node[m * n - 1].hpath, node[m * n - 1].high, apdu);
			} else {
				ISOException.throwIt(SW_NO_RESULT);// �����Ȩֵ·��
			}
			break;
		case GET_MINWEIGHT:
			getmaxweight();
			byte maxhp = 1;
			for (short i = 0; i < n + m - 1; i++) {
				if (node[(short) (node[m * n - 1].hpath[i] & 0xff)].high < maxhp) {
					maxhp = (byte) node[i].high;
				}
			}
			if (maxhp < -10)
				ISOException.throwIt(SW_NO_RESULT);// �����Ȩֵ·��
			else
				printroute(node[m * n - 1].hpath, (byte) (-maxhp), apdu);
			break;
		}
	}

	private void map(APDU apdu) { // setmap-------------------
		byte[] buffer = apdu.getBuffer();
		m = buffer[ISO7816.OFFSET_P1]; // cla ins p1 p2 lc data
		n = buffer[ISO7816.OFFSET_P2];
		length = (short) (buffer[ISO7816.OFFSET_LC] & 0xff);
		node = new Node[length];
		flag = new byte[length];
		power = new byte[length];
		hp = new short[length];
		path = new byte[n * m];
		Util.arrayCopyNonAtomic(buffer, (short) 5, power, (short) 0, length); // ���ݸ���
		if (length != m * n)
			ISOException.throwIt(SW_WRONG_LC); // ����LC
		if (m > 15 || n > 15 || m < 0 || n < 0)
			ISOException.throwIt(SW_WRONG_MN); // �������
		for (short i = 0; i < length; i++) {
			node[i] = new Node();
			node[i].weight = power[i];
			if (power[i] > 10 || power[i] < -10)
				ISOException.throwIt(SW_WRONG_WEIGHT); // ����Ȩֵ
		}
	}

	private void getmaxweight() {
		node[0].high = node[0].weight;
		node[0].hpath[0] = 0;
		for (short i = 1; i < node.length; i++) {
			if (i < n) {
				// ��һ�У�ֻ����������ǰһ���ڵ�
				node[i].high = (byte) (node[i - 1].high + node[i].weight);
				Util.arrayCopyNonAtomic(node[i - 1].hpath, (short) 0,
						node[i].hpath, (short) 0, (short) i);
				node[i].hpath[i] = (byte) i;
			} else if (i % n == 0) {
				// ��һ�У�ֻ����������ǰn���ڵ㣨�Ϸ��ڵ㣩
				node[i].high = (byte) (node[i - n].high + node[i].weight);
				Util.arrayCopyNonAtomic(node[i - n].hpath, (short) 0,
						node[i].hpath, (short) 0, (short) (i / n));
				node[i].hpath[i / n] = (byte) i;
			} else {// �ձ�������Ա�ǰn����ǰһ���ڵ㣬ȡ����
				if (node[i - 1].high > node[i - n].high) {
					node[i].high = (byte) (node[i - 1].high + node[i].weight);
					Util.arrayCopyNonAtomic(node[i - 1].hpath, (short) 0,
							node[i].hpath, (short) 0, (short) (i / n + i % n));
					node[i].hpath[i / n + i % n] = (byte) i;
				} else {
					node[i].high = (byte) (node[i - n].high + node[i].weight);
					Util.arrayCopyNonAtomic(node[i - n].hpath, (short) 0,
							node[i].hpath, (short) 0, (short) (i / n + i % n));
					node[i].hpath[i / n + i % n] = (byte) i;
				}
			}
		}
	}

	private boolean getroute(short root) {
		path[(short) (root & 0xff) / n + (short) (root & 0xff) % n] = (byte) root;
		if (root == 0) {
			hp[root / n + root % n] = node[root].weight;
		} else {
			hp[(short) (root & 0xff) / n + (short) (root & 0xff) % n] = (byte) (hp[(short) (root & 0xff)
					/ n + (short) (root & 0xff) % n - 1] + node[(short) (root & 0xff)].weight);
			if (hp[(short) (root & 0xff) / n + (short) (root & 0xff) % n] < (byte) 0) {
				return false;
			}
		}
		// else {
		if (root == n * m - 1) {
			return true;
		}

		if ((short) (root & 0xff) >= n * (m - 1)) {// ���һ��
			if (!getroute((short) (root + 1))) {
				return false;
			} else {
				return true;
			}
		} else if ((short) (root & 0xff) % n == n - 1) {// ���һ��
			if (!getroute((short) (root + n))) {
				return false;
			} else {
				return true;
			}
		} else {// �ձ����
			if (!getroute((short) ((short) (root & 0xff) + 1))) {
				if (!getroute((short) ((short) (root & 0xff) + n))) {
					return false;
				} else {
					return true;
				}
			} else {
				return true;
			}
		}
		// }
	}

	private boolean getroutelong() {
		short j = 0;
		hp[0] = power[0];
		path[0] = 0;
		if (power[0] + power[1] < 0 && power[0] + power[n] < 0) {
			return false;
		}
		for (short i = 0; i < n * m - 1; i++) {
			flag[i] = 2;
		}
		for (short i = (short) (n * (m - 1)); i < n * m - 1; i++) {
			flag[i] = 3;
		}
		for (short i = (short) (n - 1); i < n * m - 1; i += n) {
			flag[i] = 4;
		}
		while (j != n * m - 1) {
			path[j / n + j % n] = (byte) j;
			switch (flag[j]) {
			case 0:
				j = (short) (path[j / n + j % n - 1] & 0xff);
				if (flag[0] == 0)
					j = (short) (n * m);
				break;
			case 1:
				hp[j + n] = (short) (hp[j] + power[j + n]);
				if (hp[j + n] < 0)
					flag[j] = 0;
				else
					j += n;
				break;
			case 2:
				hp[j + 1] = (short) (hp[j] + power[j + 1]);
				if (hp[j + 1] < 0)
					flag[j] = 1;
				else
					j++;
				break;
			case 3:
				hp[j + 1] = (short) (hp[j] + power[j + 1]);
				if (hp[j + 1] < 0)
					flag[j] = 0;
				else
					j++;
				break;
			case 4:
				hp[j + n] = (short) (hp[j] + power[j + n]);
				if (hp[j + n] < 0)
					flag[j] = 0;
				else
					j += n;
				break;
			}
		}
		if (j == m * m)
			return false;
		else
			return true;
	}

	private void printroute(byte[] route, short weight, APDU apdu) {
		byte[] result = apdu.getBuffer();
		result[0] = (byte) (weight / 128);
		result[1] = (byte) (weight - result[0] * 128);
		for (short i = 1; i < m + n; i++) {// ת��Ϊ�������
			result[2 * i] = (byte) ((short) (route[i - 1] & 0xff) / n);// ������ȡ��
			result[2 * i + 1] = (byte) ((short) (route[i - 1] & 0xff) % n);// ������ȡ��
		}
		apdu.setOutgoingAndSend((short) 0, (short) (2 * (m + n) - 1));
	}
}