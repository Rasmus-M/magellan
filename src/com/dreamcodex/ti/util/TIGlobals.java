package com.dreamcodex.ti.util;

import java.awt.Color;

public class TIGlobals
{
	public static final Color TI_COLOR_TRANSPARENT = new Color(255, 255, 255, 0);
	public static final Color TI_COLOR_BLACK       = new Color(  0,   0,   0);//000000
	public static final Color TI_COLOR_GREY        = new Color(204, 204, 204);//CCCCCC
	public static final Color TI_COLOR_WHITE       = new Color(255, 255, 255);//FFFFFF
	public static final Color TI_COLOR_RED_LIT     = new Color(255, 121, 120);//FF7978
	public static final Color TI_COLOR_RED_MED     = new Color(252,  85,  84);//FC5554
	public static final Color TI_COLOR_RED_DRK     = new Color(212,  82,  77);//D4524D
	public static final Color TI_COLOR_YELLOW_LIT  = new Color(230, 206, 128);//E6CE80
	public static final Color TI_COLOR_YELLOW_DRK  = new Color(212, 193,  84);//D4C154
	public static final Color TI_COLOR_GREEN_LIT   = new Color( 94, 220, 120);//5EDC78
	public static final Color TI_COLOR_GREEN_MED   = new Color( 33, 200,  66);//21C842
	public static final Color TI_COLOR_GREEN_DRK   = new Color( 33, 176,  59);//21B03B
	public static final Color TI_COLOR_CYAN        = new Color( 66, 235, 245);//42EBF5
	public static final Color TI_COLOR_BLUE_LIT    = new Color(125, 118, 252);//7D76FC
	public static final Color TI_COLOR_BLUE_DRK    = new Color( 84,  85, 237);//5455ED
	public static final Color TI_COLOR_MAGENTA     = new Color(201,  91, 186);//C95BBA

    public static final Color TI_COLOR_TRANSOPAQUE = new Color(212, 232, 255);
	public static final Color TI_COLOR_UNUSED      = new Color(160, 160, 160);

	public static final Color[] TI_PALETTE =
	{
		TI_COLOR_TRANSPARENT,
		TI_COLOR_BLACK,
		TI_COLOR_GREEN_MED,
		TI_COLOR_GREEN_LIT,
		TI_COLOR_BLUE_DRK,
		TI_COLOR_BLUE_LIT,
		TI_COLOR_RED_DRK,
		TI_COLOR_CYAN,
		TI_COLOR_RED_MED,
		TI_COLOR_RED_LIT,
		TI_COLOR_YELLOW_DRK,
		TI_COLOR_YELLOW_LIT,
		TI_COLOR_GREEN_DRK,
		TI_COLOR_MAGENTA,
		TI_COLOR_GREY,
		TI_COLOR_WHITE
	};

	public static final Color[] TI_PALETTE_OPAQUE =
	{
		TI_COLOR_TRANSOPAQUE,
		TI_COLOR_BLACK,
		TI_COLOR_GREEN_MED,
		TI_COLOR_GREEN_LIT,
		TI_COLOR_BLUE_DRK,
		TI_COLOR_BLUE_LIT,
		TI_COLOR_RED_DRK,
		TI_COLOR_CYAN,
		TI_COLOR_RED_MED,
		TI_COLOR_RED_LIT,
		TI_COLOR_YELLOW_DRK,
		TI_COLOR_YELLOW_LIT,
		TI_COLOR_GREEN_DRK,
		TI_COLOR_MAGENTA,
		TI_COLOR_GREY,
		TI_COLOR_WHITE
	};

    public static final String[] TI_PALETTE_NAMES =
	{
		"Transparent",
		"Black",
		"Medium Green",
		"Light Green",
		"Dark Blue",
		"Light Blue",
		"Dark Red",
		"Cyan",
		"Medium Red",
		"Light Red",
		"Dark Yellow",
		"Light Yellow",
		"Dark Green",
		"Magenta",
		"Grey",
		"White"
	};

	public static final Integer[] TI_PALETTE_SELECT_VALUES =
	{
		0,
		1,
		2,
		3,
		4,
		5,
		6,
		7,
		8,
		9,
		10,
		11,
		12,
		13,
		14,
		15
	};

	public static final char[] CHARMAP =
	{
		' ', '!', '"', '#', '$', '%', '&', '\'',
		'(', ')', '*', '+', ',', '-', '.', '/',
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', ':', ';', '<', '=', '>', '?',
		'@', 'A', 'B', 'C', 'D', 'E', 'F', 'G',
		'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
		'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
		'X', 'Y', 'Z', '[', '\\', ']', '^', '_',
		'`', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
		'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
		'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
		'x', 'y', 'z', '{', '|', '}', '~', (char)127
	};

    public static String[] DEFAULT_TI_CHARS =
    {
        "0000000000000000",
        "0010101010100010",
        "0028282800000000",
        "0028287C287C2828",
        "0038545038145438",
        "0060640810204C0C",
        "0020505020544834",
        "0008081000000000",
        "0008102020201008",
        "0020100808081020",
        "000028107C102800",
        "000010107C101000",
        "0000000000301020",
        "000000007C000000",
        "0000000000003030",
        "0000040810204000",
        "0038444444444438",
        "0010301010101038",
        "003844040810207C",
        "0038440418044438",
        "00081828487C0808",
        "007C407804044438",
        "0018204078444438",
        "007C040810202020",
        "0038444438444438",
        "003844443C040830",
        "0000303000303000",
        "0000303000301020",
        "0008102040201008",
        "0000007C007C0000",
        "0020100804081020",
        "0038440408100010",
        "0038445C545C4038",
        "003844447C444444",
        "0078242438242478",
        "0038444040404438",
        "0078242424242478",
        "007C40407840407C",
        "007C404078404040",
        "003C40405C444438",
        "004444447C444444",
        "0038101010101038",
        "0004040404044438",
        "0044485060504844",
        "004040404040407C",
        "00446C5454444444",
        "00446464544C4C44",
        "007C44444444447C",
        "0078444478404040",
        "0038444444544834",
        "0078444478504844",
        "0038444038044438",
        "007C101010101010",
        "0044444444444438",
        "0044444428281010",
        "0044444454545428",
        "0044442810284444",
        "0044442810101010",
        "007C04081020407C",
        "0038202020202038",
        "0000402010080400",
        "0038080808080838",
        "0000102844000000",
        "000000000000007C",
        "0000201008000000",
        "00000038447C4444",
        "0000007824382478",
        "0000003C4040403C",
        "0000007824242478",
        "0000007C4078407C",
        "0000007C40784040",
        "0000003C405C4438",
        "00000044447C4444",
        "0000003810101038",
        "0000000808084830",
        "0000002428302824",
        "000000404040407C",
        "000000446C544444",
        "0000004464544C44",
        "0000007C4444447C",
        "0000007844784040",
        "0000003844544834",
        "0000007844784844",
        "0000003C40380478",
        "0000007C10101010",
        "0000004444444438",
        "0000004444282810",
        "0000004444545428",
        "0000004428102844",
        "0000004428101010",
        "0000007C0810207C",
        "0018202040202018",
        "0010101000101010",
        "0030080804080830",
        "0000205408000000",
        "0000000000000000"
    };

    public static final int MIN_CHAR = 0;
	public static final int MAX_CHAR = 1023;
    public static final int MIN_SPRITE = 0;
    public static final int MAX_SPRITE = 63;
	public static final int BASIC_FIRST_CHAR = 32;
	public static final int BASIC_LAST_CHAR = BASIC_FIRST_CHAR + (8 * 16) - 1;
	public static final int EXP_FIRST_CHAR = 0;
	public static final int EXP_LAST_CHAR = 255;
	public static final int SUPER_FIRST_CHAR = 0;
	public static final int SUPER_LAST_CHAR = 1023;
	public static final int CHARMAPSTART = 32;
	public static final int CHARMAPEND = 127;
	public static final int SPACECHAR = 32;
	public static final int CUSTOMCHAR = BASIC_FIRST_CHAR + (8 * 12);
	public static final int FINALXBCHAR = BASIC_FIRST_CHAR + (8 * 14) - 1;
	public static final int N_CHARS = (MAX_CHAR - MIN_CHAR) + 1;
	public static final int COLOR_SET_SIZE = 8;
	public static final int COLOR_SETS = N_CHARS / COLOR_SET_SIZE;
}
