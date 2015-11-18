package de.dfki.omm.types;

/** Provides input for the issuing of a certificate via KeyStore. */
public class CertificateIssuer { // TODO This class is not used anywhere
	
    public String country;
    public String organization;
    public String organizationalUnit;
    public String distinguishedNameQualifier;
    public String state_provinceName;
    public String commonName;// (e.g., "Susan Housley");
    /** Contains the issue text as a comma-separated key/value list. */
    public String subjectString;
    
    /** Adds an entry to the {@link #subjectString}, for example "C=Germany". 
     * @param tag The abbreviation for the entry, for example "C".
     * @param value The value of the entry, for example "Germany".
     */
    private void addEntryToSubjectString(String tag, String value){
    	subjectString = subjectString.concat(tag).concat("=").concat(value).concat(", ");
    }
    
    /** Constructor. The issue text can be found in {@link #subjectString}. 
     * @param country 
     * @param organization
     * @param organizationalUnit
     * @param distinguishedNameQualifier
     * @param state_provinceName
     * @param commonName
     */
    public CertificateIssuer(String country,
    	String organization,
    	String organizationalUnit,
    	String distinguishedNameQualifier,
    	String state_provinceName,
    	String commonName)
    {
    	this.country = country;
    	this.organization = organization;
    	this.organizationalUnit = organizationalUnit;
    	this.distinguishedNameQualifier = distinguishedNameQualifier;
    	this.state_provinceName = state_provinceName;
    	this.commonName = commonName;
    	this.subjectString= new String();
    	
    	addEntryToSubjectString("C", country);
    	addEntryToSubjectString("O", organization);
    	addEntryToSubjectString("OU", organizationalUnit);
    	addEntryToSubjectString("DN", distinguishedNameQualifier);
    	addEntryToSubjectString("OU", organizationalUnit);
    	addEntryToSubjectString("ST", state_provinceName);
    	addEntryToSubjectString("CN", commonName);
    }
}
