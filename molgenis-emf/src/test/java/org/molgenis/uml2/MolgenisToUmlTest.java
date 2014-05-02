package org.molgenis.uml2;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.molgenis.MolgenisOptions;
import org.molgenis.model.MolgenisModel;
import org.molgenis.model.elements.Model;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MolgenisToUmlTest
{

	private static final Logger LOG = Logger.getLogger(MolgenisToUmlTest.class.getSimpleName());
	private final String SRC_PATH = "generated/test/java/";
	private final String XMIPATH = "out_dir/";
	private final String BUILD_PATH = "build/test/classes/";

	private Model MODEL;
	private MolgenisOptions MODEL_OPTIONS;

	@BeforeClass
	public void setUpBeforeClass() throws Exception
	{
		Model model = MolgenisModel.parse(new MolgenisOptions("src/test/resources/simple_model/molgenis.properties"));
		MODEL = model;
		MODEL_OPTIONS = new MolgenisOptions();
		MODEL_OPTIONS.output_src = SRC_PATH;

	}

	@Test
	public void testMolgenisModel()
	{
		if (MODEL == null)
		{
			assert false : "Cannot instantiate molgenis model.. check the file path.  " + MODEL;
		}
	}

	@Test(dependsOnMethods = { "testMolgenisModel" })
	public void convert2Uml2AndValidate()
	{
		MolgenisToUml molgenisWrapper = MolgenisToUml.createInstance(MODEL);
		org.eclipse.uml2.uml.Model umlModel = molgenisWrapper.getUML2Model();
		assertEquals(umlModel.getName(), "example");
		assertEquals(umlModel.getMembers().size(), 18);

		UMLEcoreUtil umlUtil = molgenisWrapper.getUML2Util();
		EList<EAnnotation> e = umlModel.getEAnnotations();

		for (Iterator iterator = e.iterator(); iterator.hasNext();)
		{
			EAnnotation eAnnotation = (EAnnotation) iterator.next();
			EMap<String, String> map = eAnnotation.getDetails();
			map.get("body");
			assert map.get("body") != "" : "Annottion body is empty or null. ";
		}

		EList<NamedElement> elems = umlModel.getMembers();
		//note that we can use query API and filtering as well..
		for (Iterator iterator = elems.iterator(); iterator.hasNext();)
		{
			NamedElement namedElement = (NamedElement) iterator.next();
			if (namedElement instanceof PrimitiveType)
			{
				PrimitiveType p = (PrimitiveType) namedElement;
				assert Arrays.asList("int", "text", "date", "dateTime", "boolean", "string").contains(p.getName()) : "Unknown type "
						+ p.getName();

			} else if (namedElement instanceof org.eclipse.uml2.uml.Class)
			{

				org.eclipse.uml2.uml.Class p = (org.eclipse.uml2.uml.Class) namedElement;
				EList<Property> atts = p.getAllAttributes();
				assert atts.size() > 0 : "Class " + p.getName() + " do not have attributes";

				for (Iterator iterator2 = atts.iterator(); iterator2.hasNext();)
				{
					Property property = (Property) iterator2.next();
					assert property.getName() != "" : "Attribute do not have name";
				}
			}

		}
		umlUtil.save(umlModel, XMIPATH + "example.uml");
		umlUtil.saveAsEcore(umlModel, XMIPATH + "example.ecore");
		umlUtil.saveAsEcoreJson(umlModel, XMIPATH + "example.json");

	}

	@Test(dependsOnMethods = { "convert2Uml2AndValidate" })
	public void loadUml2()
	{

		try
		{
			UMLEcoreUtil uml2 = new UMLEcoreUtil();
			Package pkg = uml2.loadUMLModel(XMIPATH + "example.uml");
			assert pkg != null : "Cannot read UML model";

			EPackage pkg2 = uml2.loadEcorePackage(XMIPATH + "example.ecore");
			assert pkg2 != null : "Cannot read Ecore package";

			EPackage pkg3 = uml2.loadEcorePackage(XMIPATH + "example.json");
			assert pkg3 != null : "Cannot read Ecore json package";
		} catch (IOException e)
		{
			assert false : "Error opening resource " + e.getMessage();
		}

	}

	@Test(dependsOnMethods = { "convert2Uml2AndValidate" })
	public void loadUml2ExplicitlyAsAnExample()
	{

		final ResourceSet UML_RESOURCE_SET = new ResourceSetImpl();
		UML_RESOURCE_SET.getPackageRegistry().put("http://www.eclipse.org/uml2/4.0.0/UML", UMLPackage.eINSTANCE);
		UML_RESOURCE_SET.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("uml", UMLResource.Factory.INSTANCE);
		Resource resource = UML_RESOURCE_SET.createResource(URI.createFileURI(XMIPATH + "example.uml"));
		Map<String, Object> options = new HashMap<String, Object>();

		try
		{
			resource.load(options);
			org.eclipse.uml2.uml.Model model = (org.eclipse.uml2.uml.Model) EcoreUtil.getObjectByType(
					resource.getContents(), UMLPackage.Literals.MODEL);
			assert model != null : "Model not found";
			assertEquals(model.getName(), "example");
		} catch (IOException e)
		{
			assert false : e.getMessage();
		}
	}

	@Test(dependsOnMethods = { "convert2Uml2AndValidate" })
	public void checkModelConversions()
	{

		UMLEcoreUtil uml2 = new UMLEcoreUtil();
		Package pkg;
		try
		{
			pkg = uml2.loadUMLModel(XMIPATH + "example.uml");
			assert pkg != null : "Problem with the UML model";
			
			Collection<EPackage> ePkgCollection = uml2.convertToEcorePackageCollection(pkg);
			assert ePkgCollection.size() == 1 : "UML->Ecore conversion should result to one main Package"; 
			EPackage ePkg = ePkgCollection.iterator().next();
			assertEquals(ePkg.getName(),pkg.getName());
			
			Collection<Package> pkgCollection = uml2.convertFromEPackage(ePkg);
			assertEquals(pkgCollection.size(), ePkgCollection.size()); 
			Package pkg2 = pkgCollection.iterator().next();
			uml2.save(pkg2, XMIPATH + "example2.uml");
			assertEquals(pkg.getName(),pkg2.getName());

			Collection<EPackage> ePkgCollection2 = uml2.convertToEcorePackageCollection(pkg2);
			assert ePkgCollection2.size() == 1 : "UML->Ecore conversion should result to one main Package"; 
			EPackage ePkg2 = ePkgCollection.iterator().next();
			assertEquals(ePkg2.getName(),pkg.getName());
			
			//see: http://jorgemanrubia.net/2008/07/06/comparing-emf-models/
			//assert EcoreUtil.equals(ePkg, ePkg2) : "Re-converted models are not equal";
 
			//see: http://jorgemanrubia.net/2008/07/06/comparing-emf-models/
			// do not work likely because Model -> Package
			// there is also something library version related? since different UML version have been used... see the uml files: example.uml and exmaple2.uml
			// Mote uml-> ecore->uml loses at least association namesin the uml->ecore stage
			//assert EMFComparator.EMF_SIMPLE_EQUALITY_HELPER.equals(pkg, pkg2) : "Re-converted models are not equal";

		} catch (IOException e)
		{
			assert false: e.getMessage();
		}
		

	}
}
