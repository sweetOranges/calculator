import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 * 计算器内核
 * 
 * @author so
 * 
 */
public class Calculator {

	private String expression;// 表达式原始格式
	private Stack<String> ops = new Stack<String>();// 操作栈
	private Stack<String> vals = new Stack<String>();// 值栈
	private HashMap<String, Method> map = new HashMap<String, Method>();// 运算方法栈
	private ArrayList<String> expressions = new ArrayList<String>();// 存储分析表达式后的元素
	private HashMap<Type, Class<?>> types = new HashMap<Type, Class<?>>();// 存储数据类型

	/**
	 * 自定义运算方法库
	 * 
	 * @author so
	 * 
	 */
	private static class DIYLIB {
		@SuppressWarnings("unused")
		public static double add(double a, double b) {
			return a + b;
		}

		@SuppressWarnings("unused")
		public static double divide(double a, double b) {
			return a / b;
		}

		@SuppressWarnings("unused")
		public static double subtract(double a, double b) {
			return a - b;
		}

		@SuppressWarnings("unused")
		public static double multiply(double a, double b) {
			return a * b;
		}
	}

	/**
	 * 构造器
	 * 
	 * @param expression
	 *            计算表达式
	 */
	public Calculator(String expression) {
		this.expression = expression;
		this.initHeartMap();
		this.prepare();
	}

	/**
	 * 加载初始化核心库
	 */
	private void initHeartMap() {
		try {

			types.put(Double.TYPE, Double.class);
			types.put(Integer.TYPE, Integer.class);
			types.put(Long.TYPE, Long.class);

			Method[] methods = Math.class.getDeclaredMethods();
			for (int i = 0; i < methods.length; i++) {
				this.map.put(methods[i].getName(), methods[i]);
			}
			this.map.put("+",
					DIYLIB.class.getMethod("add", Double.TYPE, Double.TYPE));
			this.map.put("/",
					DIYLIB.class.getMethod("divide", Double.TYPE, Double.TYPE));
			this.map.put("-", DIYLIB.class.getMethod("subtract", Double.TYPE,
					Double.TYPE));
			this.map.put("*", DIYLIB.class.getMethod("multiply", Double.TYPE,
					Double.TYPE));
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 系统准备阶段 拆分表达式
	 */
	private void prepare() {

		char[] cs = this.expression.toCharArray();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < cs.length; i++) {
			// 循环检查字母
			for (int j = i; (j < cs.length)
					&& ((cs[j] >= 'A' && cs[j] <= 'Z') || (cs[j] >= 'a' && cs[j] <= 'z')); j++) {
				buffer.append(cs[j]);
				while (j + 1 < cs.length && cs[j + 1] >= '0'
						&& cs[j + 1] <= '9') {
					buffer.append(cs[j + 1]);
					j++;
				}
				i = j + 1;

			}
			// 循环检查数字
			for (int k = i; (k < cs.length) && ((cs[k] >= '0' && cs[k] <= '9')); k++) {
				buffer.append(cs[k]);
				i = k;
			}
			// 循环检查其他字符
			if (cs[i] == '+' || cs[i] == '-' || cs[i] == '*' || cs[i] == '+'
					|| cs[i] == '/' || cs[i] == '(' || cs[i] == ')') {
				if (buffer.length() != 0) {// 保证运算符之前的操作数必须是独立的
					this.expressions.add(buffer.toString());// 添加结果到集合中
					buffer.delete(0, buffer.length());// 清空字符
				}
				buffer.append(cs[i]);
			}
			this.expressions.add(buffer.toString());// 添加结果到集合中
			buffer.delete(0, buffer.length());// 清空字符
		}

	}

	/**
	 * 系统开始计算
	 * 
	 * @return 计算结果
	 */
	public String start() {
		for (int i = 0; i < this.expressions.size(); i++) {
			String item = this.expressions.get(i);
			if (item.matches("^[/+-/*/]$") || item.matches("^[a-z]+[0-9]*$")) {// 处理运算
				this.ops.push(item);
			}
			if (item.matches("^[0-9]*$")) {// 处理数字
				this.vals.push(item);
			}
			if (item.equals(")")) {
				String op = this.ops.pop();
				this.doit(op);
			}
		}
		this._doit("^[/*/]$");
		this._doit("^[/+-]$");

		if (this.vals.size() == 1) {
			return this.vals.pop();
		}

		return "error";
	}

	/**
	 * 分步计算实现2
	 * 
	 * @param regx
	 *            需要匹配的正则
	 */
	private void _doit(String regx) {
		for (int k = 0; k < this.ops.size(); k++) {
			String op = this.ops.get(k);
			if (op.matches("^[/+-]$")) {
				Method method = this.map.get(op);
				try {
					if (k + 1 < this.vals.size()) {
						String val = method.invoke(method.getClass(),
								Double.valueOf(this.vals.get(k)),
								Double.valueOf(this.vals.get(k + 1)))
								.toString();
						this.vals.set(k, val);
						this.vals.remove(k + 1);
						this.ops.remove(k);
						k--;
					}
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 分部计算实现
	 * 
	 * @param op
	 *            操作符
	 * @return 计算结果
	 */
	private void doit(String op) {
		Method method = this.map.get(op);
		Class<?>[] types = method.getParameterTypes();
		Object[] objects = new Object[types.length];
		for (int j = types.length - 1; j >= 0; j--) {
			Object object = null;
			Class<?> number = this.types.get(types[j]);
			try {
				Method mt = number.getDeclaredMethod("valueOf", String.class);
				object = mt.invoke(mt.getClass(), this.vals.pop());
			} catch (NoSuchMethodException | SecurityException
					| IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}
			objects[j] = object;

		}
		try {
			this.vals
					.push(method.invoke(method.getClass(), objects).toString());
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Calculator calculator = new Calculator(args[0]);
		System.out.println(calculator.start());
	}
}
