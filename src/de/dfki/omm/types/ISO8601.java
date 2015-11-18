/**
 * 
 */
package de.dfki.omm.types;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Signals an integer parsing exception.
 *
 * @author Simeon Simeonov (simeons@allaire.com)
 * @version 1.0
 * @see com.allaire.util.IntegerParser
 */
class IntegerParserException extends Exception
{
	private static final long serialVersionUID = -7189189972367016026L;    
};


/**
 * Parses unsigned integers from a string.
 *
 * @author Simeon Simeonov (simeons@allaire.com)
 * @version 1.0
 */
class IntegerParser
{
    ///////////////////////////////////////////////////////////////////////
    //
    // Implementation data
    //
    ///////////////////////////////////////////////////////////////////////


    private CharacterIterator m_it;
    private int m_radix;
    

    ///////////////////////////////////////////////////////////////////////
    //
    // Construction/Finalization
    //
    ///////////////////////////////////////////////////////////////////////


    /**
     * Constructs an IntegerParser to parse numbers in a given radix and
     * binds it to a CharacterIterator.
     *
     * @param it Iterator to input stream.
     * @param radix Radix to convert numbers from.
     */
    public IntegerParser(CharacterIterator it, int radix)
    {
        m_it = it;
        m_radix = radix;
    }
    

    ///////////////////////////////////////////////////////////////////////
    //
    // Operations
    //
    ///////////////////////////////////////////////////////////////////////


    /**
     * Parse an integer at current iterator position.
     *
     * @return Parsed integer.
     * @param minLength Minimum character length of the parsed integer.
     * @param maxLength Maximum character length of the parsed integer.
     * @exception de.dfki.pizza.tools.IntegerParserException
     *      Signals that an integer could not be parsed given the 
     *      parsing requirements.
     * @see #parseInt(int minLength, int maxLength, char chTerminator)
     * @see #parseInt(int minLength, int maxLength, boolean bCheckTerminator, char chTerminator)
     */
    public int parseInt(int minLength, int maxLength) throws IntegerParserException
    {
        return parseInt(minLength, maxLength, false, '\0');
    }
    
    
    /**
     * Parse an integer at current iterator position.
     *
     * @return Parsed integer.
     * @param minLength Minimum character length of the parsed integer.
     * @param maxLength Maximum character length of the parsed integer.
     * @param chTerminator Character at which parsing must terminate.
     * @exception de.dfki.pizza.tools.IntegerParserException
     *      Signals that an integer could not be parsed given the 
     *      parsing requirements.
     * @see #parseInt(int minLength, int maxLength)
     * @see #parseInt(int minLength, int maxLength, boolean bCheckTerminator, char chTerminator)
     */
    public int parseInt(int minLength, int maxLength, char chTerminator) throws IntegerParserException
    {
        return parseInt(minLength, maxLength, true, chTerminator);
    }
    
    
    /**
     * Parse an integer at current iterator position.
     *
     * @return Parsed integer.
     * @param minLength Minimum character length of the parsed integer.
     * @param maxLength Maximum character length of the parsed integer.
     * @param bCheckTerminator Should a check for a terminating character be made?
     * @param chTerminator Character at which parsing must terminate.
     * @exception de.dfki.pizza.tools.IntegerParserException
     *      Signals that an integer could not be parsed given the 
     *      parsing requirements.
     * @see #parseInt(int minLength, int maxLength)
     * @see #parseInt(int minLength, int maxLength, char chTerminator)
     */
    public int parseInt(int minLength, int maxLength, boolean bCheckTerminator, char chTerminator) throws IntegerParserException
    {
        int nValue = 0;
        int nLen = 0;
        char c = m_it.current();
        
        while (c != CharacterIterator.DONE)
        {
            // Get the current digit
            int nDigit = Character.digit(c, m_radix);
            if (nDigit == -1)
            {
                break;
            }
            
            // Update value
            nValue = m_radix * nValue + nDigit;
            
            // Advance
            m_it.next();
            c = m_it.current();
            ++nLen;
        }

        if (nLen < minLength || maxLength < nLen || (bCheckTerminator && c != chTerminator))
        {
            // String too short, bad character was seen, or
            // string is too long, or
            // the terminating character is not valid
            throw new IntegerParserException();
        }
        else
        {
            return nValue;
        }
    }
};


/** Handles the ISO8601 date format and conversions from and to it. 
 * 
 * @author haupert
 */
public class ISO8601 {
	
	/** Parses a ISO8601 date in full format
    *
    * <p>Some sample ISO8601 date strings:<ul>
    *      <li>1969-07-20T22:56:15-04:00</li>
    *      <li>1969-7-20T22:56:15-4:0</li></ul></p>
    *
    * @return Parsed date or null on failure
    * @param dateString ISO8601 date string
    */
   public static GregorianCalendar parseDate(String dateString)
   {        
       // Start with some believable defaults

       try
       {
           StringCharacterIterator it = new StringCharacterIterator(dateString);
           IntegerParser ip = new IntegerParser(it, 10);

           int year = ip.parseInt(4, 4, '-');
           it.next();

           int month = ip.parseInt(1, 2, '-');
           it.next();

           int day = ip.parseInt(1, 2, 'T');
           it.next();

           int hours = ip.parseInt(1, 2, ':');
           it.next();

           int minutes = ip.parseInt(1, 2, ':');
           it.next();

           int seconds = ip.parseInt(1, 2);

           int nRawTimezoneOffset = 0;
           
           char charOffset = it.current();
           if (charOffset != CharacterIterator.DONE)
           {
               if (charOffset != '+' && charOffset != '-')
               {
                   // Error: bad timezone character
                   return null;
               }

               it.next();
               
               nRawTimezoneOffset = 60 * ip.parseInt(1, 2, ':');
               it.next();

               nRawTimezoneOffset = 60 * 1000 * (nRawTimezoneOffset + ip.parseInt(1, 2));
               
               if (charOffset == '-')
               {
                   nRawTimezoneOffset *= -1;
               }

               if (it.current() != CharacterIterator.DONE)
               {
                   // Error: garbage at end
                   return null;
               }
           }

           // Get current timezone offset
           // __SIM: need DST adjustment!
           int nCurrentTimezoneOffset = TimeZone.getTimeZone("UTC").getRawOffset();
           
           // Calculate net timezone offset
           int nNetTimezoneOffset = nCurrentTimezoneOffset - nRawTimezoneOffset;
           
           GregorianCalendar gc = new GregorianCalendar(year, month - 1, day, hours, minutes, seconds);
           gc.setTimeZone(TimeZone.getTimeZone("UTC"));
           gc.add(Calendar.MILLISECOND, nNetTimezoneOffset);

           return gc;
       }
       catch(Exception e)
       {
           return null;
       }
   }
   
   /** Returns the current time in GMT as a ISO8601 String.
	 * @return ISO8601 formatted date. 
	 */
	public static String getISO8601StringWithGMT()
	{
		GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		return getISO8601String(calendar);
	}
	
	/** Given a date as a calendar object, retrieves the corresponding ISO8601 String. 
	 * @param calendar The date as a {@link GregorianCalendar}. 
	 * @return ISO8601 formatted date. 
	 */
	public static String getISO8601String(GregorianCalendar calendar)
	{
		String timeStamp = addLeadingZeros(calendar.get(GregorianCalendar.YEAR), 4);
		timeStamp += "-" + addLeadingZeros(calendar.get(GregorianCalendar.MONTH) + 1, 2);
		timeStamp += "-" + addLeadingZeros(calendar.get(GregorianCalendar.DAY_OF_MONTH), 2);
		timeStamp += "T" + addLeadingZeros(calendar.get(GregorianCalendar.HOUR_OF_DAY), 2);
		timeStamp += ":" + addLeadingZeros(calendar.get(GregorianCalendar.MINUTE), 2);
		timeStamp += ":" + addLeadingZeros(calendar.get(GregorianCalendar.SECOND), 2);
		timeStamp += "+00:00";
		return timeStamp;
	}
	
	/** Given a date as a date object, retrieves the corresponding ISO8601 String. 
	 * @param date The date as a {@link Date}. 
	 * @return ISO8601 formatted date. 
	 */
	public static String getISO8601String(Date date)
	{
		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		cal.setTime(date);
		
		/*String timeStamp = addLeadingZeros(date.getYear()+1900, 4);		
		timeStamp += "-" + addLeadingZeros(date.getMonth()+1, 2);
		timeStamp += "-" + addLeadingZeros(date.getDate(), 2);
		timeStamp += "T" + addLeadingZeros(date.getHours(), 2);
		timeStamp += ":" + addLeadingZeros(date.getMinutes(), 2);
		timeStamp += ":" + addLeadingZeros(date.getSeconds(), 2);
		timeStamp += "+00:00";*/
		return getISO8601String(cal);
	}
	
	/** Converts a date in ISO8601 format to a simple time String. 
	 * @param iso8601time The original date.
	 * @return The date as a String with the format: "dd.mm.yyyy hh:mm:ss".
	 */
	public static String transformISO8601ToSimpleTime(String iso8601time)
	{
		return transformISO8601ToSimpleTime(iso8601time, 0);
	}
	
	/** Converts a date in ISO8601 format to a simple time String. 
	 * @param iso8601time The original date.
	 * @param deviation A deviation in hours (for example to handle different time zones). 
	 * @return The date as a String with the format: "dd.mm.yyyy hh:mm:ss".
	 */
	public static String transformISO8601ToSimpleTime(String iso8601time, int deviation)
	{
		GregorianCalendar cal = parseDate(iso8601time);
		cal.add(Calendar.HOUR, deviation);
		
		String retVal = "";
		
		retVal += addLeadingZeros(cal.get(Calendar.DAY_OF_MONTH), 2) + "." + addLeadingZeros((cal.get(Calendar.MONTH) + 1), 2) + "."  + cal.get(Calendar.YEAR) + " ";
		retVal += addLeadingZeros(cal.get(Calendar.HOUR_OF_DAY), 2) + ":" + addLeadingZeros(cal.get(Calendar.MINUTE), 2) + ":" + addLeadingZeros(cal.get(Calendar.SECOND), 2);
		
		return retVal;
	}
	
	/** Adds leading zeros to an Integer value and returns the result as a String. 
	 * @param originalValue The original value to which to add leading zeros. 
	 * @param leadingZeros The maximum length the output String is meant to have including leading zeros. 
	 * @return Original String with added leading zeros (if length allows). 
	 */
	private static String addLeadingZeros(int originalValue, int leadingZeros)
	{
		return addLeadingZeros(Integer.toString(originalValue), leadingZeros);
	}
	
	/** Adds leading zeros to a String. 
	 * @param originalString The original String to which to add leading zeros. 
	 * @param leadingZeros The maximum length the output String is meant to have including leading zeros. 
	 * @return Original String with added leading zeros (if length allows). 
	 */
	private static String addLeadingZeros(String originalString, int leadingZeros)
	{
		String retVal = originalString;
		
		while(retVal.length() < leadingZeros)
		{
			retVal = "0" + retVal;
		}
		
		return retVal;
	}

}
