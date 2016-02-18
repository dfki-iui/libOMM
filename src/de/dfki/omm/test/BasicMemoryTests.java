package de.dfki.omm.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import de.dfki.omm.impl.OMMImpl;
import de.dfki.omm.interfaces.OMM;
import de.dfki.omm.interfaces.OMMHeader;
import de.dfki.omm.types.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import de.dfki.omm.impl.OMMBlockImpl;
import de.dfki.omm.impl.OMMFactory;
import de.dfki.omm.impl.OMMHeaderImpl;
import de.dfki.omm.impl.rest.OMMRestImpl;
import de.dfki.omm.interfaces.OMMBlock;

/**
 * A test suite for basic memory functionality, such as creating and destroying a memory, adding and deleting blocks, handling of SecurityBlocks. 
 */
public class BasicMemoryTests {

	// memory data
	static String memoryName = "testMemory";
	static String omsURL = "http://localhost:10082/"; 
	static String omsRestURL = omsURL+"rest/"+memoryName;
	
	// memory resources
	static OMMHeaderImpl header = null;
	static OMMRestImpl omm = null;
	
	// owner credentials
	static String[] owner = { "Klar Text", "owner", "ownerpasswd" };
	static String[] secondOwner = { "Klar Text 2", "owner2", "ownerpasswd2" };
	
	// randomiser
	Random rnd = new Random();
	
	/**
	 * (Before all tests) Creates an OMM for the tests.
	 * @throws MalformedURLException if there is something wrong with the memoryName
	 */
	@BeforeClass
	public static void createMemory() throws MalformedURLException {
		
		System.out.println("createMemory");
		
		// set logging off
		//org.restlet.engine.Engine.setLogLevel(Level.OFF);
		
		// create ingredients
		String ownerString = OMMFactory.createOMMOwnerStringFromUsernamePassword(owner[0], owner[1], owner[2]);
		header = (OMMHeaderImpl) OMMHeaderImpl.create(new URLType(new URL("http://localhost:10082/rest/"+memoryName)), null);
		OMMBlock ownerBlock = OMMFactory.createOMMOwnerBlock(header, ownerString);
		assertNotNull("header should not be null", header);
		assertNotNull("ownerBlock should not be null", ownerBlock);
		
		// create memory
		OMMFactory.createOMMViaOMSRestInterface(omsURL + "mgmt/createMemory", header, ownerBlock);
		omm = new OMMRestImpl(omsRestURL, OMMRestAccessMode.CompleteDownloadUnlimited);
		
	}
	
	/**
	 * Tests whether the OMM has been created successfully by checking storage and management address in the root node.
	 * @throws IOException if something goes wrong while requesting via ClientResource
	 */
	@Test
	public void testOMMRoot() throws IOException {
		
		System.out.println("testOMMRoot");
		
		ClientResource rootCr = new ClientResource(omsRestURL); // access memory root node
		String resultText = rootCr.get().getText();

		int linkStart, linkEnd = 0;
		String storageLink, managementLink = "";
		
		linkStart = resultText.indexOf("LINK");
		linkEnd = resultText.indexOf("DISTRIBUTED");
		storageLink = resultText.substring(linkStart+7,linkEnd-3);
		linkStart = resultText.indexOf("LINK",linkEnd);
		linkEnd = resultText.indexOf("FLUSH",linkStart);
		managementLink = resultText.substring(linkStart+7,linkEnd-3);

		// tests the content of the root node /rest/memoryName
		assertTrue("storage address should be correct", storageLink.endsWith("/rest/" + memoryName + "/st"));
		assertTrue("management address should be correct", managementLink.endsWith("/rest/"+memoryName+"/mgmt"));
		
	}
	
	/**
	 * Tests block creation by adding new blocks, and checking their contents.
	 * @throws IOException if something goes wrong while requesting via ClientResource
	 */
	@Test
	public void testBlockCreation() throws IOException {
		
		System.out.println("testBlockCreation");
		
		String resultText = "";
		String payload = getRandomString(20);
		String titleString = getRandomString(20);
		String descriptionString = getRandomString(20);
		String creatorEmail = getRandomString(20);
		String creationTime =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

		// create block parameters
		OMMMultiLangText title = new OMMMultiLangText(); 
		title.put(Locale.ENGLISH, titleString);
		OMMMultiLangText description = new OMMMultiLangText(); 
		description.put(Locale.ENGLISH, descriptionString);
		OMMEntity creatorEntity = new OMMEntity("email", creatorEmail, creationTime);
		
		// add new block
		OMMBlockImpl block = (OMMBlockImpl) OMMBlockImpl.create("testID", header.getPrimaryID(), URI.create("urn:sample:testBlock"), null, title, description, null, creatorEntity, new OMMFormat("text/plain", null, null), null, new GenericTypedValue("text/plain", payload), null, null, null);
		assertEquals("block creation should yield \"OK\"", "OK", omm.addBlock(block, null).toString());
		String blockID = block.getID();
	
		// test payload
		ClientResource blockPayloadCr = new ClientResource(omsRestURL + "/st/block/"+blockID+"/payload"); // access block "1" payload
		resultText = blockPayloadCr.get().getText();
		assertEquals("read operation should yield assigned random string", payload, resultText);

		// test meta
		ClientResource blockMetaCr = new ClientResource(omsRestURL + "/st/block/"+blockID+"/meta"); // access block "1" meta
		resultText = blockMetaCr.get().getText();
		
		int start, end = 0;
		String startString, endString = "";
		
		startString = "<omm:title xml:lang=\"en\">";
		endString = "</omm:title>";
		start = resultText.indexOf(startString);
		end = resultText.indexOf(endString);
		assertEquals("read operation should yield the correct title", titleString, resultText.substring(start+startString.length(),end));
		
		startString = "<omm:description xml:lang=\"en\">";
		endString = "</omm:description>";
		start = resultText.indexOf(startString);
		end = resultText.indexOf(endString);
		assertEquals("read operation should yield the correct description", descriptionString, resultText.substring(start+startString.length(),end));
		
		startString = "<omm:creator omm:type=\"email\">";
		endString = "</omm:creator>";
		start = resultText.indexOf(startString);
		end = resultText.indexOf(endString);
		assertEquals("read operation should yield the correct creator", creatorEmail, resultText.substring(start+startString.length(),end));
		
		startString = "<omm:date omm:encoding=\"ISO8601\">";
		endString = "</omm:date>";
		start = resultText.indexOf(startString);
		end = resultText.indexOf(endString);
		assertEquals("read operation should yield the correct creation date", creationTime, resultText.substring(start+startString.length(),end));

	}
	
	/**
	 * Tests creation of two blocks at the same time by adding them via Thread, and checking their contents.
	 * @throws IOException if something goes wrong while requesting via ClientResource
	 */
	@Test
	public void testConcurrentBlockCreation() throws IOException {
		
//		System.out.println("testConcurrentBlockCreation");
		
//		String resultText = "";
//		
//		omm = new OMMRestImpl(omsRestURL, OMMRestAccessMode.SingleAccess); 
//		
//		// create two blocks 
//		String payload1 = getRandomString(20);
//		String payload2 = getRandomString(20);
//		OMMBlockImpl block1 = (OMMBlockImpl) OMMBlockImpl.create("1", null, URI.create("urn:sample:testBlock"), null, new OMMMultiLangText(), new OMMMultiLangText(), null, OMMEntity.getDummyEntity(), new OMMFormat("text/plain", null, null), null, new GenericTypedValue("text/plain", payload1), null, null, null);
//		OMMBlockImpl block2 = (OMMBlockImpl) OMMBlockImpl.create("2", null, URI.create("urn:sample:testBlock"), null, new OMMMultiLangText(), new OMMMultiLangText(), null, OMMEntity.getDummyEntity(), new OMMFormat("text/plain", null, null), null, new GenericTypedValue("text/plain", payload2), null, null, null);
//
//		// create two blocks as simultaneously as possible
//		final CyclicBarrier gate = new CyclicBarrier(3);
//		Thread addBlock1 = new Thread(){
//		    public void run(){
//		        try {
//					gate.await();
//					
//					System.out.println("thread 1 starting");
//					
//					long start = new Date().getTime();
//					String test = omm.addBlock(block1, null).toString();
//					long end = new Date().getTime();
//					System.out.println("block1 creation: "+test+", started at "+start+", finished at "+end);	
//
//				} catch (InterruptedException | BrokenBarrierException e) {
//					e.printStackTrace();
//				} 
//		}};
//		Thread addBlock2 = new Thread(){
//		    public void run(){
//		        try {
//					gate.await();
//					
//					System.out.println("thread 2 starting");
//					
//					long start = new Date().getTime();
//					String test = omm.addBlock(block2, null).toString();
//					long end = new Date().getTime();
//					System.out.println("block2 creation: "+test+", started at "+start+", finished at "+end);	
//					
//				} catch (InterruptedException | BrokenBarrierException e) {
//					e.printStackTrace();
//				}     
//		}};
//		try {
//			
//			addBlock2.start();
//			addBlock1.start();
//			gate.await(); // open gate to start threads now
//		} catch (InterruptedException | BrokenBarrierException e) {
//			e.printStackTrace();
//		} 
//	
//		// wait for threads to finish
//		try {
//			System.out.println("wait for threads to finish");
//			addBlock1.join();
//			addBlock2.join();
//			System.out.println("after wait");
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		// delete blocks in the end
//		omm.removeBlock(block1, OMMEntity.getDummyEntity());
//		omm.removeBlock(block2, OMMEntity.getDummyEntity());
		
	}
	
	/**
	 * Tests block deletion by adding a new block, checking its contents, deleting it and checking its contents again.
	 * @throws IOException if something goes wrong while requesting via ClientResource
	 */
	@Test
	public void testBlockDeletion() throws IOException {

		System.out.println("testBlockDeletion");
		
		String payload = getRandomString(20);
		String resultText = "";
		
		// add new block
		OMMBlockImpl block = (OMMBlockImpl) OMMBlockImpl.create("testID", header.getPrimaryID(), URI.create("urn:sample:testBlock"), null, new OMMMultiLangText(), null, null, OMMEntity.getDummyEntity(), new OMMFormat("text/plain", null, null), null, new GenericTypedValue("text/plain", payload), null, null, null);
		assertEquals("block creation should yield \"OK\"", "OK", omm.addBlock(block, null).toString());
		String blockID = block.getID();

		// try accessing block
		ClientResource blockPayloadCr = new ClientResource(omsRestURL + "/st/block/"+blockID+"/payload"); // access newly created block 
		resultText = blockPayloadCr.get().getText();
		assertEquals("read operation should yield assigned random string", payload, resultText);

		// delete block
		omm.removeBlock(block, OMMEntity.getDummyEntity());
//		ClientResource blockCr = new ClientResource(omsRestURL + "/st/block/"+blockID);
//		blockCr.delete();

		// try accessing deleted block
		try {
			resultText = blockPayloadCr.get().getText();
			fail("could access a previously deleted block");
		} catch (ResourceException e) {
			assertEquals("should be 501", "Not Found", e.getMessage());
		}
	}

	/**
	 * Tests creation and deletion of a large number of blocks (after one another) in order to benchmark the OMS. <br>
	 * The needed time is recorded in the JUnit tab.
	 */
	@Test
	public void testMemoryEnduranceSuccessively() {

//		System.out.println("testMemoryEnduranceSuccessively");
		
//		int runs = rnd.nextInt(10);
//		
////		System.out.println("running "+runs+" tests");
//
//		String payload = "";
//		String titleString = "";
//		String descriptionString = "";
//		String creatorEmail = "";
//		String creationTime = "";
//		OMMMultiLangText title = new OMMMultiLangText(); 
//		OMMMultiLangText description = new OMMMultiLangText(); 
//		OMMEntity creatorEntity = null;
//		
//		// add a bunch of blocks
//		for (int i = 0; i < runs; i++) {
//			
//			payload = getRandomString(rnd.nextInt(10000));
//			titleString = getRandomString(rnd.nextInt(100));
//			descriptionString = getRandomString(rnd.nextInt(1000));
//			creatorEmail = getRandomString(rnd.nextInt(10));
//			creationTime =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
//			title.put(Locale.ENGLISH, titleString);
//			description.put(Locale.ENGLISH, descriptionString);
//			creatorEntity = new OMMEntity("email", creatorEmail, creationTime);
//			String blockID = "bl" + i;
//			
//			OMMBlockImpl block = (OMMBlockImpl) OMMBlockImpl.create(blockID, header.getPrimaryID(), URI.create("urn:sample:testBlock"), null, title, description, null, creatorEntity, new OMMFormat("text/plain", null, null), null, new GenericTypedValue("text/plain", payload), null, null, null);
//			assertEquals("block creation should yield \"OK\"", "OK", omm.addBlock(block, null).toString());
//		}
//		
//		// delete them 
//		for (int i = 0; i < runs; i++) {
//			ClientResource blockCr = new ClientResource(omsRestURL + "/st/block/" + (i+1));
//			blockCr.delete();
//		}

		// needed time is recorded in the JUnit tab
	}

	/**
	 * Tests saving in binary format
	 * @throws IOException if something goes wrong while requesting via ClientResource
	 */
	@Test
	public void testBinarySaving() throws IOException {

		System.out.println("testBinarySaving with OMMRestImpl");

		// if the current omm does not have any blocks, create some
		if (omm.getAllBlocks() == null) {
			for (OMMBlock block : createSomeBlocks(5)) {
				omm.addBlock(block, block.getCreator());
			}
		}

		// save current memory to binary file
		File targetFile = new File (System.getProperty("user.dir") + "/binaryOmm.omm");
		assertTrue(OMMFactory.serializeOMM(omm, targetFile));

		// check whether file is filled with data
		assertTrue(targetFile.exists());
		long fileSize = targetFile.length();
		assertTrue(fileSize > 0);

		// load memory from binary file
		OMM ommFromBinary = OMMFactory.deserializeOMM(targetFile);

		// check whether the two memories are the same
		assertTrue(isItTheSameMemory(omm, ommFromBinary));


		// create OMMImpl
		Collection<OMMBlock> blocks = new LinkedList<OMMBlock>();
		Collection<OMMBlock> ommBlocks = omm.getAllBlocks();
		if (ommBlocks != null) {
			for (OMMBlock block : ommBlocks) {
				blocks.add(block);
			}
		}
		OMMSourceType sourceType = OMMSourceType.LocalFile;
		OMM ommImp = OMMImpl.create(header, blocks, targetFile, sourceType);

		// save created memory to binary file
		assertTrue(OMMFactory.serializeOMM(ommImp, targetFile));

		// check whether file is filled with data
		assertTrue(targetFile.exists());
		fileSize = targetFile.length();
		assertTrue(fileSize > 0);

		// load memory from binary file
		OMM ommImpFromBinary = OMMFactory.deserializeOMM(targetFile);

		// check whether the two memories are the same
		assertTrue(isItTheSameMemory(ommImp, ommImpFromBinary));
	}

	/**
	 * (After all tests) Destroys test OMM.
	 * @throws IOException if something goes wrong while requesting via ClientResource
	 */
	@AfterClass
	public static void deleteMemory() throws IOException {

		System.out.println("deleteMemory");

		// delete memory via OMMFactory URL
//		result = OMMFactory.deleteOMMViaOMSRestInterface(new URL(omsRestURL));
//		assertTrue("delete operation should yield true", result);
		
		// delete memory via OMMFactory OMMRestImpl
		omm = OMMFactory.deleteOMMViaOMSRestInterface(omm);
		assertNull("deletion should leave omm null", omm);
		
		// delete memory via ClientResource
//		String resultText = "";
//		boolean result = false;
//		ClientResource deleteCr = new ClientResource(omsRestURL);
//		resultText = deleteCr.delete().getText();
//		assertEquals("delete operation should yield the right string", "Memory "+memoryName+" successfully deleted", resultText);

		// test whether the memory's folder has been deleted

	}
	
	
	/** Helper method to create randomized Strings as example entries. 
	 * @param length of the randomized String.
	 * @return A Sring of the requested length containing random characters and numbers.  
	 */
	private String getRandomString (int length) {
		
		String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
		StringBuilder sb = new StringBuilder(length);
		
		for (int i = 0; i < length; i++) 
			sb.append(characters.charAt(rnd.nextInt(characters.length())));
		
		return sb.toString();
	}

	/** Helper method to find out whether a restored OMM is the same as its original.
	 * @param firstOmm one OMM to compare.
	 * @param secondOmm the other OMM to compare.
	 * @return true if the OMMs seem to have the same content.
	 */
	private boolean isItTheSameMemory(OMM firstOmm, OMM secondOmm) {

		// compare headers
		OMMHeader firstHeader = firstOmm.getHeader();
		OMMHeader secondHeader = firstOmm.getHeader();
		if (!firstHeader.getPrimaryID().getType().equals(secondHeader.getPrimaryID().getType())) return false;
		if (!firstHeader.getPrimaryID().getValue().toString().equals(secondHeader.getPrimaryID().getValue().toString())) return false;
		if (firstHeader.getVersion() != secondHeader.getVersion()) return false;

		// compare blocks
		Collection<OMMBlock> fblocks = firstOmm.getAllBlocks();
		Collection<OMMBlock> sblocks = secondOmm.getAllBlocks();
		// if both memories have a block list, compare blocks
		if (fblocks != null && sblocks != null) {
			OMMBlock[] firstBlocks = fblocks.toArray(new OMMBlock[firstOmm.getAllBlocks().size()]);
			OMMBlock[] secondBlocks = sblocks.toArray(new OMMBlock[secondOmm.getAllBlocks().size()]);
			if (firstBlocks.length != secondBlocks.length) return false;
			for (int i = 0; i < firstBlocks.length; i++) {
				OMMBlock firstBlock = firstBlocks[i];
				OMMBlock secondBlock = secondBlocks[i];
				if (!firstBlock.getJsonRepresentation().equals(secondBlock.getJsonRepresentation())) return false;
			}
		}
		// if neither memory has a block list return true, else return false
		else if (fblocks == null)
			return (sblocks == null);
		else return false;

		// if you got here, everything seems to be the same
		return true;
	}

	/** Helper method to create a list of randomized OMMBlocks as example entries.
	 * @param number of the blocks to be created.
	 * @return A List<OMMBlock> of the requested amount containing random entries.
	 */
	private List<OMMBlock> createSomeBlocks(int number) {

		List<OMMBlock> blocklist = new Vector<OMMBlock>();

		for (int i = 0; i < number; i++) {

			// create block content
			String resultText = "";
			String payload = getRandomString(20);
			String titleString = getRandomString(20);
			String descriptionString = getRandomString(20);
			String creatorEmail = getRandomString(20);
			String creationTime =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

			// create block parameters
			OMMMultiLangText title = new OMMMultiLangText();
			title.put(Locale.ENGLISH, titleString);
			OMMMultiLangText description = new OMMMultiLangText();
			description.put(Locale.ENGLISH, descriptionString);
			OMMEntity creatorEntity = new OMMEntity("email", creatorEmail, creationTime);

			// create and add block
			OMMBlockImpl block = (OMMBlockImpl) OMMBlockImpl.create("testID", header.getPrimaryID(), URI.create("urn:sample:testBlock"), null, title, description, null, creatorEntity, new OMMFormat("text/plain", null, null), null, new GenericTypedValue("text/plain", payload), null, null, null);
			blocklist.add(block);

		}

		return blocklist;
	}
}
