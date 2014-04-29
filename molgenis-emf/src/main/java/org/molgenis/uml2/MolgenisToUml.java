package org.molgenis.uml2;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.Interface;
import org.eclipse.uml2.uml.InterfaceRealization;
import org.eclipse.uml2.uml.LiteralUnlimitedNatural;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.StructuralFeature;
import org.eclipse.uml2.uml.UMLPackage;
import org.molgenis.fieldtypes.EnumField;
import org.molgenis.fieldtypes.MrefField;
import org.molgenis.fieldtypes.XrefField;
import org.molgenis.model.MolgenisModelException;
import org.molgenis.model.elements.DBSchema;
import org.molgenis.model.elements.Entity;
import org.molgenis.model.elements.Field;
import org.molgenis.model.elements.Model;

/*
 * TODO
 * = MRef (chk)
 * = Add Model validation (e.g. error if same field got twice from interface realization)
 *   - https://eclipse.googlesource.com/emf-validation/org.eclipse.emf.validation/+/v20100428-2315%5E/org.eclipse.emf.validation.examples.adapter/src/org/eclipse/emf/validation/examples/adapter/EValidatorAdapter.java
 *   - http://dev.eclipse.org/svnroot/modeling/org.eclipse.mdt.papyrus/trunk/plugins/infra/services/org.eclipse.papyrus.infra.services.validation/src/org/eclipse/papyrus/infra/services/validation/EValidatorAdapter.java
 *   - https://eclipse.googlesource.com/emf-validation/org.eclipse.emf.validation/+/v200811192338%5E/org.eclipse.emf.validation.examples.adapter/src/org/eclipse/emf/validation/examples/adapter
 *   - http://www.se.eecs.uni-kassel.de/fileadmin/se/courses/SE207/EValidatorAdapter.java
 *   How to handle possible problems like overlapping field realized from different interfaces ?
 *   
 *   ks luodaanko turhia Enum objekteja... reuse existing
 *   tulevatko assosiaatiot mos kentiksi. ls uml xmi
 */
public class MolgenisToUml
{

	static Logger logger = Logger.getLogger(MolgenisToUml.class.getSimpleName());
	private HashMap<String, Classifier> clzMap = new HashMap<String, Classifier>();
	private UMLEcoreUtil uml2 = new UMLEcoreUtil();
	private Model molgenisModel;
	
	
	private  MolgenisToUml( Model model) {
		super();
		molgenisModel = model;
	}
	
	public static MolgenisToUml createInstance( Model model) { 
		MolgenisToUml wrapper = new MolgenisToUml(model);			
		return wrapper;
	}
	/*
	 * Convert Molgnis model to UML Model assumptions: - Molgenis support only
	 * single inheritance - Can implement more than one interfaces
	 */
	
	public UMLEcoreUtil getUML2Util( ) { 
		return uml2;		
	}

	/*
	 * Returns new instance of UML model
	 */
	public org.eclipse.uml2.uml.Model getUML2Model( ) { 
		return toUMLModel(molgenisModel);		
	}

	/*
	 * Returns new instance of UML model
	 */
	public EPackage getAsEcorePackage( ) { 
		org.eclipse.uml2.uml.Model m =  toUMLModel(molgenisModel);	
		Collection<EPackage> x = uml2.convertToEcorePackageCollection(m);
		assert( !x.isEmpty());
		return x.iterator().next();
	}

	public Model getMolgenisModel( ) { 
		return molgenisModel;		
	}
	
	private org.eclipse.uml2.uml.Model toUMLModel( final Model model)
	{

		Vector<Entity> entities = model.getEntities();
		org.eclipse.uml2.uml.Model pkg = uml2.createModel(model.getName());
		for (Iterator iterator = entities.iterator(); iterator.hasNext();)
		{
			Entity entity = (Entity) iterator.next();
			if (!entity.isAssociation())
			{
				// We skip association entities (MRef)
				toUMLClass(pkg, entity);
			}
		}
		annotate(pkg, model);
		return pkg;
	}

	protected boolean isInterface( final Entity e)
	{
		return e.isAbstract();
	}

	protected Classifier getClass( final org.eclipse.uml2.uml.Package container, final Entity entity)
	{
		if (clzMap.containsKey(uniqueName(entity)))
		{
			return clzMap.get(uniqueName(entity));
		} else
		{
			if (isInterface(entity))
			{
				logger.debug("Entity " + entity.getName() + "  is interface");
				Classifier cls = uml2.createInterface(container, entity.getName());
				clzMap.put(uniqueName(entity), cls);
				return cls;
			} else
			{
				Classifier cls = uml2.createClass(container, entity.getName(), entity.isAbstract());
				clzMap.put(uniqueName(entity), cls);
				return cls;
			}
		}
	}

	protected String makeUrl( final String url)
	{
		try
		{
			return URLDecoder.decode("http://example.org/"+url, "UTF-8");
		} catch (UnsupportedEncodingException e)
		{
			logger.fatal("Cannot encode url for "+url);
			throw new RuntimeException(e);
		}
	}

	protected void annotate( final Classifier feature, final Entity entity)
	{
		if ( entity.getDescription().equals("")) return;
		EAnnotation anno = feature.createEAnnotation(makeUrl( entity.getName())); // TODO:																									// annotations
		anno.getDetails().put("body", entity.getDescription());
	}

	protected void annotate( final org.eclipse.uml2.uml.Model feature, final Model model)
	{
		if ( model.getDBDescription().equals("")) return;
		EAnnotation anno = feature.createEAnnotation(makeUrl(model.getName())); // TODO:																								// annotations
		anno.getDetails().put("body", model.getDBDescription());
	}

	protected void annotate( final Association feature, final Field field)
	{
		if ( field.getDescription().equals("")) return;
		EAnnotation anno = feature.createEAnnotation( makeUrl( field.getName())); // TODO:																								// annotations
		anno.getDetails().put("body", field.getDescription());
	}

	protected void annotate( final StructuralFeature feature, final Field field)
	{
		if ( field.getDescription().equals("")) return;
		EAnnotation anno = feature.createEAnnotation(makeUrl(field.getName())); // TODO:																								// annotations
		anno.getDetails().put("body", field.getDescription());
	}

	protected Classifier toUMLClass( final org.eclipse.uml2.uml.Package container, final Entity entity)
	{

		Classifier cls = getClass(container, entity);

		if (hasText(entity.getDescription()))
		{
			annotate(cls, entity);
		}
		if (entity.hasAncestor())
		{
			Entity parent = entity.getAncestor();
			/* Molgenis supports only single inheritance */
			Classifier generalClassifier = getClass(container, parent);
			Generalization gener = uml2.createGeneralization(cls, generalClassifier);
		}
		if (entity.hasImplements())
		{
			try
			{
				Vector<Entity> im = entity.getImplements();
				for (Iterator iterator = im.iterator(); iterator.hasNext();)
				{
					Entity iFace = (Entity) iterator.next();
					Classifier iFaceClassifier = getClass(container, iFace);
					if (iFaceClassifier instanceof Interface)
					{
						if (cls instanceof Class)
						{
							logger.debug(entity.getName() + " implements: " + iFace.getName());
							InterfaceRealization real = uml2.createInterfaceRealization((Class) cls, iFace.getName(),
									(Interface) iFaceClassifier);
						} else if (cls instanceof Interface)
						{
							// todo: check this
							logger.warn("Interface " + entity.getName() + " extends interface: " + iFace.getName()
									+ " this is not tested.. Plase check");
							Generalization real = uml2.createGeneralization(cls, iFaceClassifier);
						} else
						{
							throw new RuntimeException("Only classes and interfaces are supported...");
						}
					} else
					{
						throw new RuntimeException("Not sure about molgenis model. " + iFace.getName()
								+ " should have been interface");
					}
				}
			} catch (MolgenisModelException e)
			{
				logger.error("Entity " + entity.getName() + " is inconsitent. Exception: " + e.getMessage());
			}
		}
		try
		{
			Vector<Field> fields = entity.getAllFields();

			for (Iterator iterator = fields.iterator(); iterator.hasNext();)
			{
				Field field = (Field) iterator.next();
				if (existInSuper(field, entity))
					continue; // need this for skipping also interface
								// attributes... othewriwse getLocalFields could
								// have been used

				int minCardinality = field.isNillable() ? 0 : 1;

				if (field.getType() instanceof MrefField || field.getType() instanceof XrefField)
				{

					if (field.getType() instanceof MrefField)
					{
						Classifier end2Class = getClass(container, field.getXrefField().getEntity());
						// Example: Field(entity=Test, name=Sample_id,
						// type=mref[Sample.Id], mref_name=Test_Sample_id,
						// mref_localid=Test,
						// qmref_remoteid=Sample_id, xref_label='Name',
						// auto=false, nillable=false, readonly=false, default=)
						// Create name for other end. TODO:  Add option for adding intermediating classes
						String end2Name = uncapitalize(field.getMrefLocalid()) + "For" + capitalize(field.getLabel());
						int maxCardinality = LiteralUnlimitedNatural.UNLIMITED;
						int maxEnd2Cardinality = LiteralUnlimitedNatural.UNLIMITED;

						Association asso = uml2.createAssociation(cls, true, AggregationKind.NONE_LITERAL,
								field.getLabel(), minCardinality, maxCardinality, end2Class, true,
								AggregationKind.NONE_LITERAL, end2Name, 0, maxEnd2Cardinality);
						asso.setName(entity.getName()+"_"+end2Class.getName());
						annotate(asso, field);
						// asso.setName(field.getMrefName());

					} else
					{
						Classifier end2Class = getClass(container, field.getXrefEntity());
						int maxCardinality = field.getType() instanceof MrefField ? LiteralUnlimitedNatural.UNLIMITED
								: 1;
						int maxEnd2Cardinality = LiteralUnlimitedNatural.UNLIMITED;
						String end2Name = "";
						Association asso = uml2.createAssociation(cls, true, AggregationKind.NONE_LITERAL,
								field.getLabel(), minCardinality, maxCardinality, end2Class, false,
								AggregationKind.NONE_LITERAL, end2Name, 0, maxEnd2Cardinality);
						annotate(asso, field);
						asso.setName(entity.getName()+"_"+end2Class.getName());
						// asso.setName(field.getName());
					}
				} else
				{
					DataType type = null;
					if (field.getType() instanceof EnumField)
					{
						type = uml2.createEnumeration(container, field.getName(),
								field.getEnumOptions().toArray(new String[field.getEnumOptions().size()]));
					} else
					{
						type = uml2.createPrimitiveType(container, field.getType().getXsdType());
					}
					Property attr = uml2.createAttribute(cls, field.getName(), type, minCardinality, 1);
					if (hasText(field.getDescription()))
					{
						annotate(attr, field);
					}
				}

			}
		} catch (MolgenisModelException e)
		{
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
		return cls;
	}

	/*
	 * Utility methods
	 */
	public static final String uncapitalize( final String originalStr)
	{
		return xcapitalize(originalStr,true, new Locale("en", "US"));
	}
	public static final String capitalize( final String originalStr)
	{
		return xcapitalize(originalStr,false, new Locale("en", "US"));
	}

	public static final String xcapitalize( final String originalStr, boolean uncap, final Locale locale)
	{
		final int splitIndex = 1;
		final String result;
		if (originalStr.isEmpty())
		{
			result = originalStr;
		} else
		{
			final String first = uncap ? originalStr.substring(0, splitIndex).toLowerCase(locale) : originalStr.substring(0, splitIndex).toUpperCase(locale);
			final String rest = originalStr.substring(splitIndex);
			final StringBuilder uncapStr = new StringBuilder(first).append(rest);
			result = uncapStr.toString();
		}
		return result;
	}

	protected String uniqueName( final Entity entity)
	{
		return entity.getNamespace() + "/" + entity.getName();
	}

	protected boolean hasText( final String str)
	{
		return str != null && str.trim().equals("");
	}

	protected String harmonizeString( final String a)
	{
		if (a == null)
			return "";
		String _a = a.replaceAll("^\\s+", "").replaceAll("\\s+$", "").replaceAll("\\s+", " ");
		return _a;
	}

	private boolean existInSuper( final Field field, final Entity entity)
	{
		boolean ex = false;

		try
		{
			final List<Entity> parents = entity.getAllAncestors();
			final List<Entity> impls = entity.getAllImplements();
			for (Entity pare : parents)
			{
				List<Field> f = pare.getLocalFields();
				for (int i = 0; (i < f.size() && !ex); i++)
				{
					ex = harmonizeString(f.get(i).getName()).equals(harmonizeString(field.getName()));
				}
				if (ex)
				{
					continue;
				}
			}

			if (!ex)
			{
				for (Entity pare : impls)
				{
					List<Field> f = pare.getLocalFields();
					for (int i = 0; (i < f.size() && !ex); i++)
					{
						ex = harmonizeString(f.get(i).getName()).equals(harmonizeString(field.getName()));
					}
					if (ex)
					{
						continue;
					}
				}

			}

		} catch (MolgenisModelException e1)
		{
			throw new RuntimeException(e1);
		}

		if (ex && (field.getType() instanceof MrefField || field.getType() instanceof XrefField))
		{
			logger.warn("Not sure how XREFs should be handled... see e.g. in existsInSuper method");
		}
		return ex;
	}

}
