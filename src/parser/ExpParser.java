package parser;

import core.StructDefine;
import file.SheetReader;

public class ExpParser {
	private SheetReader sheetReader;
	private StructDefine.Position position;
    //4�ֱ������
    public static final int NONE_TOKEN = 0;    //���Ϊ�ջ��߽�����
    public static final int DELIMITER_TOKEN = 1;    //���Ϊ�ָ���
    public static final int VARIABLE_TOKEN = 2;    //���Ϊ����
    public static final int NUMBER_TOKEN = 3;    //���Ϊ����
    public static final int FUNCTION_TOKEN = 4;		//���Ϊ����

    //4�ִ�������
    public static final int SYNTAX_ERROR = 0;    //�﷨����
    public static final int UNBALPARENS_ERROR = 1;    //����û�н�������
    public static final int NOEXP_ERROR = 2;    //���ʽΪ�մ���
    public static final int DIVBYZERO_ERROR = 3;    //��0������

    //���4�ִ������Ͷ����4��������ʾ
    public static final String[] ERROR_MESSAGES = {"Syntax Error", "Unbalanced Parentheses", "No Expression Present", "Division by Zero"};

    //���ʽ�Ľ������
    public static final String EOE = "#";
    
    private String exp; //���ʽ�ַ���
    private int expIndex; //��������ǰָ���ڱ��ʽ�е�λ��
    private String token; //��������ǰ����ı��
    private int tokenType; //��������ǰ����ı������

    public ExpParser(SheetReader sheetReader, StructDefine.Position pos) {
    	this.sheetReader = sheetReader;
    	this.position = pos;
    }

    /**
     * ����һ�����ʽ�����ر��ʽ��ֵ
     */
    public double evaluate(String expStr) throws Exception {
        double result;
        this.exp = expStr;
        this.expIndex = 0;

        //��ȡ��һ�����
        this.getToken();
        if (this.token.equals(EOE)) {
            //û�б��ʽ�쳣
            this.handleError(NOEXP_ERROR);
        }

        result = this.parseAssign(); //����ֵ���
        //�����긳ֵ��䣬Ӧ�þ��Ǳ��ʽ��������������ǣ��򷵻��쳣
        if (!this.token.equals(EOE)) {
        	System.err.println(expStr);
            this.handleError(SYNTAX_ERROR);
        }
        return result;
    }

    /**
     * ����ֵ���
     */
    public double parseAssign() throws Exception {
        double result; //���
        String oldToken; //�ɱ��
        int oldTokenType; //�ɱ�ǵ�����

        //�����������Ǳ���
        if (this.tokenType == VARIABLE_TOKEN) {
            //���浱ǰ���
            oldToken = new String(this.token);
            oldTokenType = this.tokenType;
            //�����һ�����
            this.getToken();
            //�����ǰ��ǲ��ǵȺţ�
            if (!this.token.equals("=")) {
                this.putBack(); //�ع�
                //����һ����ֵ��䣬����ǻָ�����һ�����
                this.token = new String(oldToken);
                this.tokenType = oldTokenType;
            } else {
            	//�����ǰ����ǵȺţ�������������ֵ����ʽ�磺a = 3 + 5;
                //�����Ⱥź�����ʽ��ֵ��Ȼ���ٽ��õ���ֵ��������
                this.getToken();
                //��Ϊ�Ӽ��������ȼ���ͣ����Լ���Ӽ������ʽ
                result = this.parseAddOrSub();
                return result;
            }
        }
        //�����ǰ������Ͳ��Ǳ��������߲��Ǹ�ֵ��䣬���üӼ���������ʽ��ֵ
        return this.parseAddOrSub();
    }

    /**
     * ����Ӽ������ʽ
     */
	private double parseAddOrSub() throws Exception {
        char op; //�����
        double result; //���
        double partialResult; //�ӱ��ʽ�Ľ��

        result = this.pareseMulOrDiv(); //�ó˳������㵱ǰ���ʽ��ֵ
        //�����ǰ��ǵĵ�һ����ĸ�ǼӼ��ţ���������мӼ�����
        while ((op = this.token.charAt(0)) == '+' || op == '-') {
            this.getToken(); //ȡ��һ�����
            //�ó˳������㵱ǰ�ӱ��ʽ��ֵ
            partialResult = this.pareseMulOrDiv();
            switch (op) {
                case '-':
                    //����Ǽ����������Ѵ�����ӱ��ʽ��ֵ��ȥ��ǰ�ӱ��ʽ��ֵ
                    result = result - partialResult;
                    break;
                case '+':
                    //����Ǽӷ������Ѵ�����ӱ��ʽ��ֵ���ϵ�ǰ�ӱ��ʽ��ֵ
                    result = result + partialResult;
                    break;
            }
        }
        return result;
    }

    /**
     * ����˳������ʽ������ȡģ����
     */
    private double pareseMulOrDiv() throws Exception {
        char op; //�����
        double result; //���
        double partialResult; //�ӱ��ʽ���
        //��ָ��������㵱ǰ�ӱ��ʽ��ֵ
        result = this.parseExponent();
        //�����ǰ��ǵĵ�һ����ĸ�ǳˡ�������ȡģ���㣬��������г˳�������
        while ((op = this.token.charAt(0)) == '*' || op == '/' || op == '%') {
            this.getToken(); //ȡ��һ���
            //��ָ��������㵱ǰ�ӱ��ʽ��ֵ
            partialResult = this.parseExponent();
            switch (op) {
                case '*':
                    //����ǳ˷��������Ѵ����ӱ��ʽ��ֵ���Ե�ǰ�ӱ��ʽ��ֵ
                    result = result * partialResult;
                    break;
                case '/':
                    //����ǳ������жϵ�ǰ�ֱ��ʽ��ֵ�Ƿ�Ϊ0�����Ϊ0�����׳���0���쳣
                    if (partialResult == 0.0) {
                    	result = 0;
                        //this.handleError(DIVBYZERO_ERROR);
                    }
                    //������Ϊ0������г�������
                    else result = result / partialResult;
                    break;
                case '%':
                    //�����ȡģ���㣬ҲҪ�жϵ�ǰ�ӱ��ʽ��ֵ�Ƿ�Ϊ0
                    if (partialResult == 0.0) {
                        this.handleError(DIVBYZERO_ERROR);
                    }
                    result = result % partialResult;
                    break;
            }
        }
        return result;
    }

    /**
     * ����ָ�����ʽ
     */
    private double parseExponent() throws Exception {
        double result; //���
        double partialResult; //�ӱ��ʽ��ֵ
        double ex; //ָ���ĵ���
        int t; //ָ������

        //��һԪ������㵱ǰ�ӱ��ʽ��ֵ��������
        result = this.parseUnaryOperator();
        //�����ǰ���Ϊ��^��,��Ϊָ������
        if (this.token.equals("^")) {
            //��ȡ��һ��ǣ������ָ������
            this.getToken();
            partialResult = this.parseExponent();
            ex = result;
            if (partialResult == 0.0) {
                //���ָ������Ϊ0����ָ����ֵΪ1
                result = 1.0;
            } else {
                //����ָ����ֵΪ����Ϊָ���ݵĵ�����˵Ľ��
                for (t = (int) partialResult - 1; t > 0; t--) {
                    result = result * ex;
                }
            }
        }
        return result;
    }

    /**
     * ����һԪ���㣬����������ʾ�����͸���
     */
    private double parseUnaryOperator() throws Exception {
        double result; //���
        String op; //�����
        op = "";
        //�����ǰ�������Ϊ�ָ��������ҷָ�����ֵ����+����-
        if ((this.tokenType == DELIMITER_TOKEN) && this.token.equals("+") || this.token.equals("-")) {
            op = this.token;
            this.getToken();
        }
        //������������㵱ǰ�ӱ��ʽ��ֵ
        result = this.parseBracket();
        if (op.equals("-")) {
            //��������Ϊ-�����ʾ���������ӱ��ʽ��ֵ��Ϊ����
            result = -result;
        }
        return result;
    }

    /**
     * ������������
     */
    private double parseBracket() throws Exception {
        double result; //���
        //�����ǰ���Ϊ�����ţ����ʾ��һ����������
        if (this.token.equals("(")) {
            this.getToken(); //ȡ��һ���
            result = this.parseAddOrSub(); //�üӼ�����������ӱ��ʽ��ֵ
            //�����ǰ��ǲ����������ţ��׳����Ų�ƥ���쳣
            if (!this.token.equals(")")) {
                this.handleError(UNBALPARENS_ERROR);
            }
            this.getToken(); //����ȡ��һ�����
        } else {
            //������������ţ���ʾ����һ���������㣬����ԭ��Ԫ����������ӱ��ʽֵ
            result = this.parseAtomElement();
        }
        return result;
    }

    /**
     * ����ԭ��Ԫ�����㣬��������������
     */
    private double parseAtomElement() throws Exception {
        double result = 0.0; //���
        switch (this.tokenType) {
            case NUMBER_TOKEN:
                //�����ǰ�������Ϊ����
                try {
                    //�����ֵ��ַ���ת��������ֵ
                    result = Double.parseDouble(this.token);
                } catch (NumberFormatException exc) {
                    this.handleError(SYNTAX_ERROR);
                }
                this.getToken(); //ȡ��һ�����
                break;
            case VARIABLE_TOKEN:
                //�����ǰ��������Ǳ�������ȡ������ֵ
                result = this.findVar(token);
                this.getToken();
                break;
            case FUNCTION_TOKEN:
            	FuncCalculation fc = new FuncCalculation(this.token, sheetReader, position);
            	result = fc.getResult();
            	this.getToken();
            	break;
            default:
                this.handleError(SYNTAX_ERROR);
                break;
        }
        return result;
    }

    
    /**
     * ���ݱ�������ȡ������ֵ��������������ȴ���1����ֻȡ�����ĵ�һ���ַ�
     */
    private double findVar(String vname) throws Exception {
        //��sheet��ȡ���ñ�����ֵ
    	String[] temp = vname.split("\\[|]"); 
    	int row = Integer.parseInt(temp[1]) + position.GetRow();
    	int column = Integer.parseInt(temp[3]) + position.GetColumn();
    	if(row < 0 || column < 0 || row >= sheetReader.getRowCount() || column >= sheetReader.getColumnCount())
    		return 0;
    	if(sheetReader.getCells()[row][column].getValueType() == 0)
    		return Double.parseDouble(sheetReader.getCells()[row][column].getValue());
    	else
    		return 0;
    }

    /**
     * �ع�������������ǰָ����ǰ�Ƶ���ǰ���λ��
     */
    private void putBack() {
        if (this.token == EOE) {
            return;
        }
        //��������ǰָ����ǰ�ƶ�
        for (int i = 0; i < this.token.length(); i++) {
            this.expIndex--;
        }
    }

    /**
     * �����쳣���
     */
    private void handleError(int errorType) throws Exception {
        //�����쳣���ʱ�����ݴ������ͣ�ȡ���쳣��ʾ��Ϣ������ʾ��Ϣ��װ���쳣���׳�
        throw new Exception(ERROR_MESSAGES[errorType]);
    }

    /**
     * ��ȡ��һ�����
     */
    private void getToken() {
        //���ó�ʼֵ
        this.token = "";
        this.tokenType = NONE_TOKEN;

        //�����ʽ�Ƿ�����������������ǰָ���Ѿ��������ַ������ȣ�
        //��������ʽ�Ѿ��������õ�ǰ��ǵ�ֵΪEOE
        if (this.expIndex == this.exp.length()) {
            this.token = EOE;
            return;
        }

        //�������ʽ�еĿհ׷�
        while (this.expIndex < this.exp.length()
                && Character.isWhitespace(this.exp.charAt(this.expIndex))) {
            ++this.expIndex;
        }

        //�ٴμ����ʽ�Ƿ����
        if (this.expIndex == this.exp.length()) {
            this.token = EOE;
            return;
        }

        //ȡ�ý�������ǰָ��ָ����ַ�
        char currentChar = this.exp.charAt(this.expIndex);
        //�����ǰ�ַ���һ���ָ���������Ϊ����һ���ָ������
        //����ǰ��Ǻͱ�����͸�ֵ������ָ�����
        if (isDelim(currentChar)) {
            this.token += currentChar;
            this.expIndex++;
            this.tokenType = DELIMITER_TOKEN;
        } 
        else if (currentChar == '{') {
        	//��{}��������ס��ʶ����Ϊһ������
        	this.expIndex++;
        	currentChar = this.exp.charAt(this.expIndex);
        	while(currentChar != '}') {
        		this.token += currentChar;
                this.expIndex++;
                if (this.expIndex >= this.exp.length()) {
                    break;
                } else {
                    currentChar = this.exp.charAt(this.expIndex);
                }
        	}
        	this.expIndex++;
        	this.tokenType = FUNCTION_TOKEN;
        }
        else if (currentChar == '<') {
        	//��<>��������ס��ʶ����Ϊһ����
        	this.expIndex++;
        	currentChar = this.exp.charAt(this.expIndex);
        	while(currentChar != '>') {
        		this.token += currentChar;
                this.expIndex++;
                if (this.expIndex >= this.exp.length()) {
                    break;
                } else {
                    currentChar = this.exp.charAt(this.expIndex);
                }
        	}
        	this.expIndex++;
        	this.tokenType = VARIABLE_TOKEN;
        } 
        else if (Character.isDigit(currentChar)) {
        	//�����ǰ�ַ���һ�����֣�����Ϊ��ǰ��ǵ�����Ϊ����
            //��������ָ����ƣ�֪������һ���ָ�����֮����ַ����Ǹ����ֵ���ɲ���
            while (!isDelim(currentChar)) {
                this.token += currentChar;
                this.expIndex++;
                if (this.expIndex >= this.exp.length()) {
                    break;
                } else {
                    currentChar = this.exp.charAt(this.expIndex);
                }
            }
            this.tokenType = NUMBER_TOKEN; //���ñ������Ϊ����
            //System.out.println(this.token);
        } 
        else {
            //�޷�ʶ����ַ�������Ϊ���ʽ����
        	this.token = EOE;
            return;
        }
    }

    /**
     * �ж�һ���ַ��Ƿ�Ϊ�ָ��� ���ʽ�е��ַ�������
     * �ӡ������������������ˡ�*��������/����ȡģ��%����ָ����^������ֵ�������������š������������š�����
     */
    private boolean isDelim(char c) {
        if (("+-*/%^=()".indexOf(c) != -1)) {
            return true;
        }
        return false;
    }
}
