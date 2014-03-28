package com.meidusa.amoeba.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.Locale;

import com.meidusa.amoeba.config.ConfigUtil;

public class CmdLineParser {

	/**
	 * Base class for exceptions that may be thrown when options are parsed
	 */
	public static abstract class OptionException extends Exception {
		private static final long serialVersionUID = 1L;

		OptionException(String msg) {
			super(msg);
		}
	}

	/**
	 * Thrown when the parsed command-line contains an option that is not
	 * recognised. <code>getMessage()</code> returns an error string suitable
	 * for reporting the error to the user (in English).
	 */
	public static class UnknownOptionException extends OptionException {
		private static final long serialVersionUID = 1L;

		UnknownOptionException(String optionName) {
			this(optionName, "Unknown option '" + optionName + "'");
		}

		UnknownOptionException(String optionName, String msg) {
			super(msg);
			this.optionName = optionName;
		}

		/**
		 * @return the name of the option that was unknown (e.g. "-u")
		 */
		public String getOptionName() {
			return this.optionName;
		}

		private String optionName = null;
	}

	/**
	 * Thrown when the parsed commandline contains multiple concatenated short
	 * options, such as -abcd, where one is unknown. <code>getMessage()</code>
	 * returns an english human-readable error string.
	 * 
	 * @author Vidar Holen
	 */
	public static class UnknownSuboptionException extends
			UnknownOptionException {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private char suboption;

		UnknownSuboptionException(String option, char suboption) {
			super(option, "Illegal option: '" + suboption + "' in '" + option
					+ "'");
			this.suboption = suboption;
		}

		public char getSuboption() {
			return suboption;
		}
	}

	/**
	 * Thrown when the parsed commandline contains multiple concatenated short
	 * options, such as -abcd, where one or more requires a value.
	 * <code>getMessage()</code> returns an english human-readable error string.
	 * 
	 * @author Vidar Holen
	 */
	public static class NotFlagException extends UnknownOptionException {
		private static final long serialVersionUID = 1L;
		private String notflag;

		NotFlagException(String option, String unflaggish) {
			super(option, "Illegal option: '" + option + "', '" + unflaggish
					+ "' requires a value");
			notflag = unflaggish;
		}

		/**
		 * @return the first character which wasn't a boolean (e.g 'c')
		 */
		public String getOptionChar() {
			return notflag;
		}
	}

	/**
	 * Thrown when an illegal or missing value is given by the user for an
	 * option that takes a value. <code>getMessage()</code> returns an error
	 * string suitable for reporting the error to the user (in English).
	 */
	public static class IllegalOptionValueException extends OptionException {
		private static final long serialVersionUID = 1L;

		public IllegalOptionValueException(Option opt, String value) {
			super("Illegal value '"
					+ value
					+ "' for option "
					+ (opt.shortForm() != null ? "-" + opt.shortForm() + "/"
							: "") + "--" + opt.longForm());
			this.option = opt;
			this.value = value;
		}

		/**
		 * @return the name of the option whose value was illegal (e.g. "-u")
		 */
		public Option getOption() {
			return this.option;
		}

		/**
		 * @return the illegal value
		 */
		public String getValue() {
			return this.value;
		}

		private Option option;
		private String value;
	}

	/**
	 * Representation of a command-line option
	 */
	public static abstract class Option<T> {

		private String shortForm = null;
		private String longForm = null;
		private boolean wantsValue = true;
		private boolean required = false;
		private String description;
		private T defaultValue = null;
		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public T getDefaultValue() {
			return defaultValue;
		}

		public void setDefaultValue(T defaultValue) {
			this.defaultValue = defaultValue;
		}

		protected Option(String longForm, boolean wantsValue) {
			this(null, longForm, wantsValue);
		}

		protected Option(char shortForm, String longForm, boolean wantsValue) {
			this((shortForm>0?String.valueOf(shortForm):null), longForm, wantsValue);
		}

		private Option(String shortForm, String longForm, boolean wantsValue) {
			if (longForm == null)
				throw new IllegalArgumentException("Null longForm not allowed");
			this.shortForm = shortForm;
			this.longForm = longForm;
			this.wantsValue = wantsValue;
		}

		public String shortForm() {
			return this.shortForm;
		}

		public String longForm() {
			return this.longForm;
		}

		public boolean isRequired() {
			return required;
		}

		public void setRequired(boolean required) {
			this.required = required;
		}

		/**
		 * Tells whether or not this option wants a value
		 */
		public boolean wantsValue() {
			return this.wantsValue;
		}

		public final T getValue(String arg, Locale locale)
				throws IllegalOptionValueException {
			if (this.wantsValue) {
				if (arg == null) {
					throw new IllegalOptionValueException(this, "");
				}
			}else{
				if(arg == null){
					return this.getDefaultValue();
				}
			}
			if(arg != null){
				arg = ConfigUtil.filter(arg);
			}
			return this.parseValue(arg, locale);
		}

		/**
		 * Override to extract and convert an option value passed on the
		 * command-line
		 */
		protected T parseValue(String arg, Locale locale)
				throws IllegalOptionValueException {
			return null;
		}
	}
	
	public static class BooleanOption extends Option<Boolean> {
		public BooleanOption(char shortForm, String longForm,boolean wantsValue,boolean requried,boolean defaultValue,String description) {
			super(shortForm, longForm, wantsValue);
			this.setDefaultValue(defaultValue);
			this.setDescription(description);
			this.setRequired(requried);
		}

		public BooleanOption(char shortForm, String longForm,boolean wantsValue,boolean requried,String description) {
			this(shortForm, longForm, wantsValue,requried,false,description);
		}
		
		public BooleanOption(char shortForm, String longForm,boolean wantsValue,boolean requried){
			this(shortForm,longForm,wantsValue, requried,null);
		}

		public BooleanOption(char shortForm, String longForm,boolean wantsValue){
			this(shortForm,longForm, wantsValue,true);
		}
		
		public BooleanOption(String longForm) {
			super(longForm, true);
		}
		
		 protected Boolean parseValue(String arg, Locale locale)
				throws IllegalOptionValueException {
			return Boolean.parseBoolean(arg);
		}
	}

	/**
	 * An option that expects an integer value
	 */
	public static class IntegerOption extends Option<Integer> {
		public IntegerOption(char shortForm, String longForm,boolean wantsValue,boolean requried,Integer defaultValue,String description) {
			super(shortForm, longForm, wantsValue);
			this.setDefaultValue(defaultValue);
			this.setDescription(description);
			this.setRequired(requried);
		}

		public IntegerOption(char shortForm, String longForm,boolean wantsValue,boolean requried,String description) {
			this(shortForm, longForm, wantsValue,requried,null,description);
		}
		
		public IntegerOption(char shortForm, String longForm,boolean wantsValue,boolean requried){
			this(shortForm,longForm, wantsValue,requried,null);
		}
		
		public IntegerOption(char shortForm, String longForm,boolean wantsValue) {
			this(shortForm, longForm, wantsValue,true);
		}

		public IntegerOption(String longForm) {
			super(longForm, true);
		}
		
		protected Integer parseValue(String arg, Locale locale)
				throws IllegalOptionValueException {
			try {
				return new Integer(arg);
			} catch (NumberFormatException e) {
				throw new IllegalOptionValueException(this, arg);
			}
		}
	}

	/**
	 * An option that expects a long integer value
	 */
	public static class LongOption extends Option<Long> {
		
		public LongOption(char shortForm, String longForm,boolean wantsValue,boolean required,Long defaultValue,String description) {
			super(shortForm, longForm, wantsValue);
			this.setDefaultValue(defaultValue);
			this.setDescription(description);
			this.setRequired(required);
		}

		public LongOption(char shortForm, String longForm,boolean wantsValue,boolean required,String description) {
			this(shortForm, longForm, wantsValue,required,null,description);
		}
		
		public LongOption(char shortForm, String longForm,boolean wantsValue,boolean required){
			this(shortForm,longForm, wantsValue,required,null);
		}
		
		public LongOption(char shortForm, String longForm,boolean wantsValue) {
			this(shortForm, longForm,wantsValue,true);
		}

		public LongOption(String longForm) {
			super(longForm, true);
		}
		
		protected Long parseValue(String arg, Locale locale)
				throws IllegalOptionValueException {
			try {
				return new Long(arg);
			} catch (NumberFormatException e) {
				throw new IllegalOptionValueException(this, arg);
			}
		}
	}

	/**
	 * An option that expects a floating-point value
	 */
	public static class DoubleOption extends Option<Double> {
		
		public DoubleOption(char shortForm, String longForm,boolean wantsValue,boolean required,Double defaultValue,String description) {
			super(shortForm, longForm, wantsValue);
			this.setDefaultValue(defaultValue);
			this.setDescription(description);
			this.setRequired(required);
		}

		public DoubleOption(char shortForm, String longForm,boolean wantsValue,boolean required,String description) {
			this(shortForm, longForm,wantsValue, required,null,description);
		}
		
		public DoubleOption(char shortForm, String longForm,boolean wantsValue,boolean required){
			this(shortForm,longForm,wantsValue, required,null);
		}
		
		public DoubleOption(char shortForm, String longForm,boolean wantsValue) {
			this(shortForm, longForm,wantsValue, true);
		}
		

		public DoubleOption(String longForm) {
			super(longForm, true);
		}

		protected Double parseValue(String arg, Locale locale)
				throws IllegalOptionValueException {
			try {
				NumberFormat format = NumberFormat
						.getNumberInstance(locale);
				Number num = (Number) format.parse(arg);
				return new Double(num.doubleValue());
			} catch (ParseException e) {
				throw new IllegalOptionValueException(this, arg);
			}
		}
	}

	/**
	 * An option that expects a string value
	 */
	public static class StringOption extends Option<String> {
		
		/**
		 * 
		 * @param shortForm
		 * @param longForm
		 * @param wantsValue --  must has an argument 
		 * @param required -- must required
		 * @param defaultValue 
		 * @param description
		 */
		public StringOption(char shortForm, String longForm,boolean wantsValue,boolean required,String defaultValue,String description) {
			super(shortForm, longForm, wantsValue);
			this.setDefaultValue(defaultValue);
			this.setDescription(description);
			this.setRequired(required);
		}

		public StringOption(char shortForm, String longForm,boolean wantsValue,boolean required,String description) {
			this(shortForm, longForm, wantsValue,required,null,description);
		}
		
		public StringOption(char shortForm, String longForm,boolean wantsValue,boolean required){
			this(shortForm,longForm,wantsValue, required,null);
		}
		
		public StringOption(char shortForm, String longForm,boolean wantsValue) {
			this(shortForm, longForm,wantsValue, true);
		}

		public StringOption(String longForm) {
			super(longForm, true);
		}
		
		public String getDefaultValue() {
			if(super.getDefaultValue() != null){
				return ConfigUtil.filter(super.getDefaultValue());
			}else{
				return super.getDefaultValue();
			}
		}

		protected String parseValue(String arg, Locale locale) {
			return arg;
		}
	}

	String name;

	public CmdLineParser(String applicationName) {
		this.name = applicationName;
	}

	/**
	 * Add the specified Option to the list of accepted options
	 */
	public final Option<?> addOption(Option<?> opt) {
		if (opt.shortForm() != null) {
			this.options.put("-" + opt.shortForm(), opt);
		}
		this.options.put("--" + opt.longForm(), opt);
		optionList.add(opt);
		if (opt.longForm() != null) {
			format.fullLenght = (format.fullLenght < opt.longForm().length() ? opt
					.longForm().length()
					: format.fullLenght);
		}
		if (opt.getDescription() != null) {
			format.descriptionLenght = (format.descriptionLenght < opt
					.getDescription().length() ? opt.getDescription().length()
					: format.descriptionLenght);
		}
		return opt;
	}

	public List<Option<?>> getOptions() {
		return optionList;
	}
	
	public Option<?> getOption(String name){
		return this.options.get("--"+name);
	}

	public final Option addOption(OptionType type, char shortForm,
			String longForm, boolean requried, String description) {
		Option option = null;
		switch (type) {
		case String:
			option = new StringOption(shortForm, longForm,true);
			break;
		case Int:
			option = new IntegerOption(shortForm, longForm,true);
			break;
		case Long:
			option = new LongOption(shortForm, longForm,true);
			break;
		case Double:
			option = new DoubleOption(shortForm, longForm,true);
			break;
		case Boolean:
			option = new BooleanOption(shortForm, longForm,true);
			break;
		}
		option.setDescription(description);
		option.wantsValue = requried;
		return addOption(option);
	}
	
	public final Option addOption(OptionType type, char shortForm,
			String longForm, boolean requried, String description,String defaultValue) {
		Option option =  addOption(type,shortForm,longForm,requried,description);
		option.setDefaultValue(defaultValue);
		return option;
	}

	/**
	 * Equivalent to {@link #getOptionValue(Option, Object) getOptionValue(o,
	 * null)}.
	 */
	public final Object getOptionValue(Option o) {
		return getOptionValue(o, o.getDefaultValue());
	}

	/**
	 * @return the parsed value of the given Option, or null if the option was
	 *         not set
	 */
	public Object getOptionValue(Option o, Object def) {
		Vector v = (Vector) values.get(o.longForm());

		if (v == null) {
			return def;
		} else if (v.isEmpty()) {
			return o.getDefaultValue();
		} else {
			Object result = v.elementAt(0);
			v.removeElementAt(0);
			return result == null?def : result;
		}
	}

	/**
	 * @return A Vector giving the parsed values of all the occurrences of the
	 *         given Option, or an empty Vector if the option was not set.
	 */
	public final Vector getOptionValues(Option option) {
		Vector result = new Vector();

		while (true) {
			Object o = getOptionValue(option, null);

			if (o == null) {
				return result;
			} else {
				result.addElement(o);
			}
		}
	}

	/**
	 * @return the non-option arguments
	 */
	public final String[] getRemainingArgs() {
		return this.remainingArgs;
	}

	/**
	 * Extract the options and non-option arguments from the given list of
	 * command-line arguments. The default locale is used for parsing options
	 * whose values might be locale-specific.
	 */
	public final void parse(String[] argv) throws IllegalOptionValueException,
			UnknownOptionException {

		// It would be best if this method only threw OptionException, but for
		// backwards compatibility with old user code we throw the two
		// exceptions above instead.

		parse(argv, Locale.getDefault());
	}

	/**
	 * Extract the options and non-option arguments from the given list of
	 * command-line arguments. The specified locale is used for parsing options
	 * whose values might be locale-specific.
	 */
	public final void parse(String[] argv, Locale locale)
			throws IllegalOptionValueException, UnknownOptionException {

		// It would be best if this method only threw OptionException, but for
		// backwards compatibility with old user code we throw the two
		// exceptions above instead.

		Vector otherArgs = new Vector();
		int position = 0;
		this.values = new Hashtable(10);
		while (position < argv.length) {
			String curArg = argv[position];
			if (curArg.startsWith("-")) {
				if (curArg.equals("--")) { // end of options
					position += 1;
					break;
				}
				String valueArg = null;
				if (curArg.startsWith("--")) { // handle --arg=value
					int equalsPos = curArg.indexOf("=");
					if (equalsPos != -1) {
						valueArg = curArg.substring(equalsPos + 1);
						curArg = curArg.substring(0, equalsPos);
					}
				} else if (curArg.length() > 2) { // handle -abcd
					for (int i = 1; i < curArg.length(); i++) {
						Option opt = (Option) this.options.get("-"
								+ curArg.charAt(i));
						if (opt == null)
							throw new UnknownSuboptionException(curArg, curArg
									.charAt(i));
						if (opt.wantsValue())
							throw new NotFlagException(curArg, new String(""+curArg.charAt(i)));
						addValue(opt, opt.getValue(null, locale));

					}
					position++;
					continue;
				}

				Option opt = (Option) this.options.get(curArg);
				if (opt == null) {
					throw new UnknownOptionException(curArg);
				}
				Object value = null;
				if (opt.wantsValue()) {
					if (valueArg == null) {
						position += 1;
						if (position < argv.length) {
							valueArg = argv[position];
						}
					}
					value = opt.getValue(valueArg, locale);
				} else {
					value = opt.getValue(null, locale);
				}

				addValue(opt, value);

				position += 1;
			} else {
				otherArgs.addElement(curArg);
				position += 1;
			}
		}
		for (; position < argv.length; ++position) {
			otherArgs.addElement(argv[position]);
		}

		this.remainingArgs = new String[otherArgs.size()];
		otherArgs.copyInto(remainingArgs);
	}

	public void checkRequired() throws NotFlagException{
		for(Option option : optionList){
			if(option.isRequired()){
				Object obj = values.get(option.longForm);
				if(obj == null){
					throw new NotFlagException(option.longForm,option.shortForm);
				}
			}
		}
	}
	
	private void addValue(Option opt, Object value) {
		String lf = opt.longForm();

		Vector v = (Vector) values.get(lf);

		if (v == null) {
			v = new Vector();
			values.put(lf, v);
		}

		v.addElement(value);
	}

	class Format {
		int fullLenght;
		int descriptionLenght;
	}

	public void printUsage() {
		try {
			BufferedWriter writer = new BufferedWriter(new PrintWriter(
					System.out));
			writer.write(this.name);
			writer.write(" [-option value/--option=value]");
			writer.newLine();
			for (Option option : optionList) {
				writer.write("    ");
				writer.write((!StringUtil.isEmpty(option.shortForm())?("-" + option.shortForm()+","):"") + "--"
						+ option.longForm()
						+ (option.wantsValue() ? "=value" :"") + (option.isRequired() ?  " [required]" : "")
						+ "");
				if (option.getDescription() != null) {
					BufferedReader read = new BufferedReader(new StringReader(
							option.getDescription()));
					String line = null;
					while ((line = read.readLine()) != null) {
						writer.newLine();
						writer.write("        ");
						writer.write(line);
					}
				}
				writer.newLine();
				writer.newLine();
			}
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Format format = new Format();
	private String[] remainingArgs = null;
	private List<Option<?>> optionList = new ArrayList<Option<?>>();
	private Hashtable<String, Option> options = new Hashtable<String, Option>(
			10);
	private Hashtable values = new Hashtable(10);

}
