package org.molgenis.uml2;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.EcorePackage.Literals;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.InterfaceRealization;
import org.eclipse.uml2.uml.LiteralUnlimitedNatural;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.util.UMLUtil;
import org.eclipselabs.emfjson.EMFJs;
import org.eclipselabs.emfjson.resource.JsResourceFactoryImpl;

/*
 * Code modified from 
 * https://subversion.assembla.com/svn/pfiste333/trunk/article-ibm-uml/src/com/ibm/uml2/articles
 * Juha Muilu
 * 
 * See:
 * http://www.ibm.com/developerworks/opensource/library/os-eclipse-dynamicemf/
 */
public class UMLEcoreUtil
{

	static Logger logger = Logger.getLogger(UMLEcoreUtil.class.getSimpleName());
	protected final ResourceSet UML_RESOURCE_SET = new ResourceSetImpl();
	protected final ResourceSet ECORE_RESOURCE_SET = new ResourceSetImpl();
	protected final ResourceSet JSON_RESOURCE_SET = new ResourceSetImpl();

	public static class ResourceKind
	{
		private String kind;

		ResourceKind(String kind)
		{
			this.kind = kind;
		}

		@Override
		public String toString()
		{
			return kind;
		}
	}

	public final static ResourceKind UML = new ResourceKind(UMLResource.FILE_EXTENSION); // it is uml
	public final static ResourceKind ECORE = new ResourceKind("ecore");
	public final static ResourceKind ECORE_JSON = new ResourceKind("json");

	public int MULTIP_UNLIMITED = LiteralUnlimitedNatural.UNLIMITED;
	public int AGGREGATION_COMPOSITE = AggregationKind.COMPOSITE;
	public AggregationKind AGGREGATION_COMPOSITE_LITERAL = AggregationKind.COMPOSITE_LITERAL;
	public int AGGREGATION_NONE = AggregationKind.NONE;
	public AggregationKind AGGREGATION_NONE_LITERAL = AggregationKind.NONE_LITERAL;

	public UMLEcoreUtil()
	{
		super();
		init();
	}

	protected void init()
	{
		UML_RESOURCE_SET.getPackageRegistry().put("http://www.eclipse.org/uml2/4.0.0/UML", UMLPackage.eINSTANCE);
		UML_RESOURCE_SET.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(UML.toString(), UMLResource.Factory.INSTANCE);
		ECORE_RESOURCE_SET.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(ECORE.toString(), new XMIResourceFactoryImpl());
		JSON_RESOURCE_SET.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(ECORE_JSON.toString(), new JsResourceFactoryImpl());
		// for some reason above initialization do not work for json. Need to add this:
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(ECORE_JSON.toString(), new JsResourceFactoryImpl());
	}

	public Model createModel(final String name)
	{
		Model model = UMLFactory.eINSTANCE.createModel();
		model.setName(name);
		return model;
	}

	public org.eclipse.uml2.uml.Package createPackage(final String name)
	{
		org.eclipse.uml2.uml.Package pkg = UMLFactory.eINSTANCE.createPackage();
		pkg.setName(name);
		return pkg;
	}

	public UMLPackage getUMLPackage()
	{
		org.eclipse.uml2.uml.UMLPackage pkg = UMLFactory.eINSTANCE.getUMLPackage();
		return pkg;
	}

	public org.eclipse.uml2.uml.Package createPackage(final org.eclipse.uml2.uml.Package nestingPackage,
			final String name)
	{
		org.eclipse.uml2.uml.Package pkg = nestingPackage.createNestedPackage(name);
		return pkg;
	}

	protected <A, B> boolean implementsInterface(final java.lang.Class<A> interfaceClass,
			final java.lang.Class<B> clazz2)
	{
		boolean v = interfaceClass.isAssignableFrom(clazz2);
		return v;
	}

	public <T extends NamedElement> T findNamedElement(final PackageableElement pkg, final String name,
			final java.lang.Class<T> interfaceClass)
	{
		EList<Element> l = pkg.allOwnedElements();
		T type = null;
		boolean found = false;
		for (Iterator iterator = l.iterator(); !found && iterator.hasNext();)
		{
			Element element = (Element) iterator.next();
			if (implementsInterface(interfaceClass, element.getClass()) && ((NamedElement) element).getName() == null)
			{
				// logger.info("NamedElement without name: "+element);
			} else
			{
				if (implementsInterface(interfaceClass, element.getClass())
						&& ((NamedElement) element).getName().equals(name))
				{
					type = (T) element;
					found = true;
				}
			}
		}
		return type;
	}

	public PrimitiveType createPrimitiveType(final org.eclipse.uml2.uml.Package pkg, final String name)
	{
		PrimitiveType type = findNamedElement(pkg, name, PrimitiveType.class);
		if (type != null)
		{
			logger.info("Primitive " + name + " reused ");
			return type;
		} else
		{
			PrimitiveType primitiveType = (PrimitiveType) pkg.createOwnedPrimitiveType(name);
			return primitiveType;
		}
	}

	public Enumeration createEnumeration(final org.eclipse.uml2.uml.Package pkg, final String name)
	{
		Enumeration enumeration = findNamedElement(pkg, name, Enumeration.class);
		if (enumeration != null)
		{
			return enumeration;
		} else
		{
			enumeration = (Enumeration) pkg.createOwnedEnumeration(name);
		}
		return enumeration;
	}

	public EnumerationLiteral createEnumerationLiteral(final Enumeration enumeration, final String name)
	{
		EnumerationLiteral enumerationLiteral = enumeration.createOwnedLiteral(name);
		return enumerationLiteral;
	}

	public Enumeration createEnumeration(final org.eclipse.uml2.uml.Package pkg, final String name,
			final String[] literals)
	{
		Enumeration enu = createEnumeration(pkg, name);
		for (int i = 0; i < literals.length; i++)
		{
			createEnumerationLiteral(enu, literals[i]);
		}
		return enu;
	}

	public org.eclipse.uml2.uml.Classifier createClass(final org.eclipse.uml2.uml.Package pkg, final String name,
			boolean isAbstract)
	{
		Classifier type = findNamedElement(pkg, name, Classifier.class);
		if (type != null)
		{
			logger.fatal("Class " + name + " is defined twice");
			throw new RuntimeException("Class " + name + " is defined twice");
		}
		org.eclipse.uml2.uml.Class clz = pkg.createOwnedClass(name, isAbstract);
		return clz;
	}

	public org.eclipse.uml2.uml.Classifier createInterface(final org.eclipse.uml2.uml.Package pkg, final String name)
	{
		Classifier type = findNamedElement(pkg, name, Classifier.class);
		if (type != null)
		{
			logger.fatal("Interface " + name + " is defined twice");
			throw new RuntimeException("Interface " + name + " is defined twice");
		}
		org.eclipse.uml2.uml.Classifier clz = pkg.createOwnedInterface(name);
		return clz;
	}

	public Generalization createGeneralization(final Classifier specificClassifier, final Classifier generalClassifier)
	{
		Generalization generalization = specificClassifier.createGeneralization(generalClassifier);
		return generalization;
	}

	public InterfaceRealization createInterfaceRealization(final org.eclipse.uml2.uml.Class concreteClass,
			final String name, final org.eclipse.uml2.uml.Interface interface_)
	{
		InterfaceRealization realization = concreteClass.createInterfaceRealization(name, interface_);
		return realization;
	}

	public Property createAttribute(final org.eclipse.uml2.uml.Classifier ownerClassifier, final String name,
			final Type type, final int lowerBound, final int upperBound)
	{
		if (ownerClassifier instanceof org.eclipse.uml2.uml.Class)
		{
			Property attribute = ((org.eclipse.uml2.uml.Class) ownerClassifier).createOwnedAttribute(name, type,
					lowerBound, upperBound);
			return attribute;
		} else
		{
			if (ownerClassifier instanceof org.eclipse.uml2.uml.Interface)
			{
				Property attribute = ((org.eclipse.uml2.uml.Interface) ownerClassifier).createOwnedAttribute(name,
						type, lowerBound, upperBound);
				return attribute;
			} else
			{
				String message = "Classifier " + ownerClassifier.getName() + " must be either Interface or Class";
				logger.fatal(message);
				throw new RuntimeException(message);
			}

		}
	}

	public Association createAssociation(final Type end1Class, final boolean end1IsNavigable,
			final AggregationKind end1Aggregation, final String end1Name, final int end1LowerBound,
			final int end1UpperBound, final Type end2Class, final boolean end2IsNavigable,
			final AggregationKind end2Aggregation, final String end2Name, final int end2LowerBound,
			final int end2UpperBound)
	{
		Association association = end1Class.createAssociation(end1IsNavigable, end1Aggregation, end1Name,
				end1LowerBound, end1UpperBound, end2Class, end2IsNavigable, end2Aggregation, end2Name, end2LowerBound,
				end2UpperBound);
		return association;

	}

	/*
	 * I/O
	 */

	public Resource createResource(final String fileName, final ResourceKind kind)
	{

		File file = new File(fileName);
		if ( file.exists()) {
			logger.info("Resource exist. File name = "+fileName);
		}
		URI _uri = URI.createFileURI(file.getAbsolutePath());
		return createResource(_uri, kind);
	}

	public Resource createResource(URI _uri, final ResourceKind kind)
	{

		if (kind == UML)
		{
			Resource resource = UML_RESOURCE_SET.createResource(_uri);
			return resource;
		} else if (kind == ECORE)
		{
			Resource resource = ECORE_RESOURCE_SET.createResource(_uri);
			return resource;
		} else if (kind == ECORE_JSON)
		{
			Resource resource = JSON_RESOURCE_SET.createResource(_uri);
			return resource;
		} else
		{
			String message = "Only uml and ecore file extensions are supported (filename.ecore or filename.uml)";
			logger.fatal(message);
			throw new RuntimeException(message);
		}
	}

	public String getExtension(final String name)
	{
		try
		{
			return name.substring(name.lastIndexOf("."));
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public void save(final org.eclipse.uml2.uml.Package package_, String fileName)
	{
		Resource resource = createResource(fileName, UML);

		save(package_, resource);
	}

	public void saveWithoutRoot(final org.eclipse.uml2.uml.Package package_, String fileName)
	{
		// see: http://wiki.eclipse.org/EMF/Recipes
		XMIResource resource = (XMIResource) createResource(fileName, UML);
		resource.getDefaultSaveOptions().put(XMLResource.OPTION_SUPPRESS_DOCUMENT_ROOT, true);
		save(package_, resource);
	}

	public Collection<EPackage> convertToEcorePackageCollection(final org.eclipse.uml2.uml.Package package_)
	{
		Map<String, String> prop = new HashMap<String, String>();
		Collection<EPackage> xe = org.eclipse.uml2.uml.util.UMLUtil.convertToEcore(package_, prop);
		return xe;
	}

	public Collection<org.eclipse.uml2.uml.Package> convertFromEPackage(final EPackage package_)
	{
		Map<String, String> prop = new HashMap<String, String>();
		Collection<org.eclipse.uml2.uml.Package> xe = org.eclipse.uml2.uml.util.UMLUtil.convertFromEcore(package_, prop);
		return xe;
	}

	public void saveAsEcoreJson(final org.eclipse.uml2.uml.Package package_, final String fileName)
	{
		Collection<EPackage> xe = convertToEcorePackageCollection(package_);
		if (xe.size() != 1)
		{
			String message = "Package " + package_.getName() + " should contain only one root package (the model).";
			logger.fatal(message);
		} else
		{
			Resource res = createResource(fileName, ECORE_JSON);
			EPackage ePackage = (EPackage) xe.iterator().next();
			logger.info("Saving... " + ePackage.getName());
			save(ePackage, res);
		}

	}

	public void saveAsEcore(final org.eclipse.uml2.uml.Package package_, final String fileName)
	{

		Collection<EPackage> xe = convertToEcorePackageCollection(package_);
		if (xe.size() != 1)
		{
			String message = "Package " + package_.getName() + " should contain only one root package (the model)";
			logger.fatal(message);
		} else
		{
			Resource res = createResource(fileName, ECORE);
			EPackage ePackage = (EPackage) xe.iterator().next();
			logger.info("Saving... " + ePackage.getName());
			save(ePackage, res);
		}
	}

	public void save(final EModelElement modelElement, final String fileName)
	{
		Resource resource = createResource(fileName, ECORE);
		save(modelElement, resource);
	}

	public void save(final EModelElement emodelElement, final Resource resource)
	{
		logger.info("Creating resource set. " + resource.getURI());
		EList<EObject> contents = resource.getContents();
		contents.add(emodelElement);
		// addPackageElements( emodelElement, contents);
		Map options = new HashMap();
		options.put(XMLResource.OPTION_ENCODING, "UTF-8");
		try
		{
			resource.save(options);
			logger.info(resource.getURI() + " done.");
		} catch (IOException ioe)
		{
			logger.fatal(ioe.getMessage());
		}
	}

	public org.eclipse.uml2.uml.Package loadUMLPackage(final String fileName) throws IOException
	{
		return loadUMLPackage(URI.createFileURI(fileName));
	}

	public org.eclipse.uml2.uml.Package loadUMLPackage(final URI uri) throws IOException
	{

		Resource resource = createResource(uri, UML);
		Map<String, Object> options = new HashMap<String, Object>();
		try
		{ 
			resource.load(options);
		} catch (IOException e)
		{
			logger.fatal(e.getMessage());
			throw e;
		}
		org.eclipse.uml2.uml.Package package_ = (org.eclipse.uml2.uml.Package) EcoreUtil.getObjectByType(resource.getContents(),
					UMLPackage.Literals.PACKAGE);
		if (package_ == null)
		{
			logger.info("UML package is null. Check uri=" + uri);
		}
		return package_;

	}

	public org.eclipse.uml2.uml.Model loadUMLModel(final String fileName) throws IOException
	{
		return loadUMLModel(URI.createFileURI(fileName));
	}

	public org.eclipse.uml2.uml.Model loadUMLModel(final URI uri) throws IOException
	{

		Resource resource = createResource(uri, UML);
		Map<String, Object> options = new HashMap<String, Object>();
		try
		{ 
			resource.load(options);
		} catch (IOException e)
		{
			logger.fatal(e.getMessage());
			throw e;
		}
		org.eclipse.uml2.uml.Model package_ = (org.eclipse.uml2.uml.Model) EcoreUtil.getObjectByType(resource.getContents(),
					UMLPackage.Literals.MODEL);
		if (package_ == null)
		{
			logger.info("UML package is null. Check uri=" + uri);
		}
		return package_;

	}
	
	public EPackage loadEcorePackage(final String fileName) throws IOException
	{
		return loadEcorePackage(URI.createFileURI(fileName));
	}
	
	public EPackage loadEcorePackage(final URI uri) throws IOException
	{
		Resource resource = createResource(uri, ECORE);
		Map<String, Object> options = new HashMap<String, Object>();
		try
		{
			resource.load(options);
		} catch (IOException e)
		{
			logger.fatal(e.getMessage());
			throw e;
		}
		EPackage package_ = (EPackage) EcoreUtil
				.getObjectByType(resource.getContents(), EcorePackage.Literals.EPACKAGE);
		return package_;

	}

	public EPackage loadEcoreJsonPackage(final String fileName) throws IOException
	{
		return loadEcoreJsonPackage(URI.createFileURI(fileName));
	}
	public EPackage loadEcoreJsonPackage(final URI uri) throws IOException
	{

		Resource resource = createResource(uri, ECORE_JSON);
		Map<String, Object> options = new HashMap<String, Object>();
		options.put(EMFJs.OPTION_ROOT_ELEMENT, EcoreFactory.eINSTANCE.getEPackage());	
		try
		{
			resource.load(options);
		} catch (IOException e)
		{
			logger.fatal(e.getMessage());
			throw e;
		}
		EPackage package_ = (EPackage) EcoreUtil
				.getObjectByType(resource.getContents(), EcorePackage.Literals.EPACKAGE);
		return package_;
	}

	public void addPackageElements(final EModelElement package_, final EList existingcontents)
	{
		// todo: check why this was used in the example
		for (Iterator allContents = UMLUtil.getAllContents(package_, true, false); allContents.hasNext();)
		{
			EObject eObject = (EObject) allContents.next();
			if (eObject instanceof Element)
			{
				Element elmt = (Element) eObject;
				EList<EObject> straps = elmt.getStereotypeApplications();// getStereotypeApplications(elmt);
				if (!straps.isEmpty())
				{
					existingcontents.addAll(straps);
				}
			}
		}
	}


}
