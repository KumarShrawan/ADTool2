package lu.uni.adtool.tree;

import lu.uni.adtool.domains.AdtDomain;
import lu.uni.adtool.domains.Domain;
import lu.uni.adtool.domains.SandDomain;
import lu.uni.adtool.domains.ValuationDomain;
import lu.uni.adtool.domains.adtpredefined.DiffLMH;
import lu.uni.adtool.domains.adtpredefined.DiffLMHE;
import lu.uni.adtool.domains.adtpredefined.MinCost;
import lu.uni.adtool.domains.adtpredefined.MinSkill;
import lu.uni.adtool.domains.adtpredefined.MinTimePar;
import lu.uni.adtool.domains.adtpredefined.MinTimeSeq;
import lu.uni.adtool.domains.adtpredefined.PowerCons;
import lu.uni.adtool.domains.adtpredefined.ProbSucc;
import lu.uni.adtool.domains.adtpredefined.ReachPar;
import lu.uni.adtool.domains.adtpredefined.ReachSeq;
import lu.uni.adtool.domains.adtpredefined.SatOpp;
import lu.uni.adtool.domains.adtpredefined.SatProp;
import lu.uni.adtool.domains.adtpredefined.SatScenario;
import lu.uni.adtool.domains.rings.Ring;
import lu.uni.adtool.domains.sandpredefined.MinTime;
import lu.uni.adtool.tools.Debug;
import lu.uni.adtool.tools.Options;
import lu.uni.adtool.ui.DomainDockable;
import lu.uni.adtool.ui.MainController;
import lu.uni.adtool.ui.TreeDockable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import org.reflections.Reflections;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.MultipleCDockableFactory;

public class DomainFactory implements MultipleCDockableFactory<DomainDockable, ValuationDomain> {

  public DomainFactory(MainController controller) {
    this.domainIdCount = new HashMap<Integer, Integer>();
    this.domainDockables = new HashMap<Integer, ArrayList<DomainDockable>>();
    this.controller = controller;
  }

  public String getId() {
    return DOMAIN_FACTORY_ID;
  }

  /*
   * An empty layout is required to read a layout from an XML file or from a
   * byte stream
   */
  public ValuationDomain create() {
    return new ValuationDomain();
  }

  public boolean match(DomainDockable dockable, ValuationDomain values) {
    return dockable.getValues().equals(values);
  }

  /* Called when applying a stored layout */
  public DomainDockable read(ValuationDomain values) {
    Debug.log("Reading domain treeId " + values.getTreeId() + " domainId " + values.getDomainId());
    DomainDockable dockable = new DomainDockable(this, values);
    Integer treeId = new Integer(values.getTreeId());
    Integer domainCount = domainIdCount.get(treeId);
    if (domainCount == null) {
      domainCount = new Integer(0);
    }
    domainCount = new Integer(Math.max(domainCount.intValue(), values.getDomainId()));
    domainIdCount.put(treeId, domainCount);
    ArrayList<DomainDockable> d = this.domainDockables.get(treeId);
    if (d == null) {
      d = new ArrayList<DomainDockable>();
    }
    d.add(dockable);
    Debug.log("adding tree of domains with id:" + treeId);
    this.domainDockables.put(treeId, d);
    CControl control = controller.getControl();
    TreeDockable treeDockable = (TreeDockable) controller.getControl()
        .getMultipleDockable(TreeDockable.getUniqueId(treeId.intValue()));
    if (treeDockable != null) {
      dockable.getCanvas().setTree(treeDockable.getCanvas().getTree());
      if (treeDockable.getCanvas().isSand()) {
        dockable.hideShowAll();
      }
      if (treeDockable.getLayout().getDomains().indexOf(values) == -1) {
        treeDockable.getLayout().addDomain(values);
      }
    }
    return dockable;
  }

  /* Called when storing the current layout */
  public ValuationDomain write(DomainDockable dockable) {
    return dockable.getValues();
  }

  public MainController getController() {
    return this.controller;
  }

  public int getNewUniqueId(Integer treeId) {
    Integer domainCount = domainIdCount.get(treeId);
    if (domainCount == null) {
      domainCount = new Integer(0);
    }
    domainCount = new Integer(domainCount.intValue() + 1);
    domainIdCount.put(treeId, domainCount);
    Debug.log("for treeId " + treeId + " value:" + domainCount);
    return domainCount.intValue();
  }

  @SuppressWarnings("all")
  public static Vector<Domain<?>> getPredefinedDomains(boolean forSand) {
    Vector<Domain<?>> result = new Vector<Domain<?>>();
    if (forSand) {
      Reflections reflections = new Reflections(sandDomainsPrefix);
      Set<Class<? extends SandDomain>> m = reflections.getSubTypesOf(SandDomain.class);
      for (Class<? extends SandDomain> c : m) {
        SandDomain<Ring> d = null;
        Constructor<SandDomain<Ring>>[] ct =
            (Constructor<SandDomain<Ring>>[]) c.getDeclaredConstructors();
        try {
          if (ct.length == 1) {
            d = ct[0].newInstance();
            result.add((SandDomain<Ring>) d);
          }
        }
        catch (InstantiationException e) {
          e.printStackTrace();
          return null;
        }
        catch (IllegalAccessException e) {
          System.err.println(e.getStackTrace());
          return null;
        }
        catch (InvocationTargetException e) {
          System.err.println(e.getStackTrace());
          return null;
        }
      }
      // fixing not loading classes under webstart
      if (result.size() == 0) {
        result.add(new MinTime());
      }
    }
    else {
      Reflections reflections = new Reflections(adtDomainsPrefix);
      Set<Class<? extends AdtDomain>> m = reflections.getSubTypesOf(AdtDomain.class);
      for (Class<? extends AdtDomain> c : m) {
        Debug.log(" for c:" + c);
        if (c.getSimpleName().equals("RankingDomain")) {
          continue;
        }

        AdtDomain<Ring> d = null;
        Constructor<AdtDomain<Ring>>[] ct =
            (Constructor<AdtDomain<Ring>>[]) c.getDeclaredConstructors();
        try {
          if (ct.length == 1) {
            d = ct[0].newInstance();
            result.add((AdtDomain<Ring>) d);
          }
        }
        catch (InstantiationException e) {
          e.printStackTrace();
          return null;
        }
        catch (IllegalAccessException e) {
          System.err.println(e);
          return null;
        }
        catch (InvocationTargetException e) {
          System.err.println(e);
          return null;
        }
      }
    }
    // fixing not loading classes under webstart
    if (result.size() == 0) {
      result.add(new SatProp());
      result.add(new ReachPar());
      result.add(new MinTimeSeq());
      result.add(new SatOpp());
      result.add(new MinTimePar());
      result.add(new SatScenario());
      result.add(new DiffLMH());
      result.add(new PowerCons());
      result.add(new MinSkill());
      result.add(new DiffLMHE());
      result.add(new ProbSucc());
      result.add(new ReachSeq());
      result.add(new MinCost());
    }
    return result;
  }

  public static boolean isSandDomain(String domainName) {
    String name = domainName;
    if (!domainName.startsWith(sandDomainsPrefix)) {
      name = sandDomainsPrefix + "." + domainName;
    }
    Constructor<SandDomain<Ring>>[] ct = null;
    try {
      final Class<?> c = Class.forName(name);
      ct = (Constructor<SandDomain<Ring>>[]) c.getDeclaredConstructors();
    }
    catch (ClassNotFoundException e) {
      return false;
    }
    return true;
  }

  /**
   * Creates predefined domain from string name.
   *
   * @param domainName
   *          domain class name
   * @return created domain.
   */
  @SuppressWarnings("unchecked")
  public static Domain<Ring> createFromString(String domainName) {
    String name = domainName;
    boolean isSand = isSandDomain(domainName);
    if (isSand) {
      if (!domainName.startsWith(sandDomainsPrefix)) {
        name = sandDomainsPrefix + "." + domainName;
      }
      Constructor<SandDomain<Ring>>[] ct = null;
      try {
        final Class<?> c = Class.forName(name);
        ct = (Constructor<SandDomain<Ring>>[]) c.getDeclaredConstructors();
      }
      catch (ClassNotFoundException e) {
        System.err.println(Options.getMsg("error.class.notfound") + " " + name);
        return null;
      }
      SandDomain<Ring> d = null;
      if (ct.length == 1) {
        try {
          d = ct[0].newInstance();
        }
        catch (InstantiationException e) {
          System.err.println(e);
          return null;
        }
        catch (IllegalAccessException e) {
          System.err.println(e);
          return null;
        }
        catch (InvocationTargetException e) {
          System.err.println(e);
          return null;
        }
      }
      return d;
    }
    else {
      if (domainName.startsWith(oldAdtDomainsPrefix)) {
        name = adtDomainsPrefix + domainName.substring(oldAdtDomainsPrefix.length());
        ;
      }
      else if (!domainName.startsWith(adtDomainsPrefix)) {
        name = adtDomainsPrefix + "." + domainName;
      }
      Constructor<AdtDomain<Ring>>[] ct = null;
      try {
        final Class<?> c = Class.forName(name);
        ct = (Constructor<AdtDomain<Ring>>[]) c.getDeclaredConstructors();
      }
      catch (ClassNotFoundException e) {
        System.err.println(Options.getMsg("error.class.notfound") + " " + name);
        return null;
      }
      AdtDomain<Ring> d = null;
      if (ct.length == 1) {
        try {
          d = ct[0].newInstance();
        }
        catch (InstantiationException e) {
          System.err.println(e);
          return null;
        }
        catch (IllegalAccessException e) {
          System.err.println(e);
          return null;
        }
        catch (InvocationTargetException e) {
          System.err.println(e);
          return null;
        }
      }
      return d;
    }
  }

  public void notifyAllTreeChanged(Integer id) {
    ArrayList<DomainDockable> domains = domainDockables.get(id);
    if (domains != null) {
      Debug.log("domains size:" + domains.size());
      for (DomainDockable domain : domains) {
        domain.getCanvas().treeChanged();
      }
    }
  }

  public void repaintAllDomains(Integer id) {
    ArrayList<DomainDockable> domains = domainDockables.get(id);
    if (domains != null) {
      for (DomainDockable domain : domains) {
        domain.getCanvas().repaint();
      }
    }
  }

  public void removeDomain(DomainDockable dockable) {
    ArrayList<DomainDockable> d = domainDockables.get(new Integer(dockable.getCanvas().getId()));
    if (d != null) {
      d.remove(dockable);
    }
  }

  public ArrayList<DomainDockable> getDomains(Integer treeId) {
    return domainDockables.get(treeId);
  }

  /**
   * Get domain class name as string.
   *
   * @param d
   *          domain.
   * @return domain class name.
   */
  public static String getClassName(SandDomain<Ring> d) {
    return d.getClass().getSimpleName();
  }

  public static final String                          DOMAIN_FACTORY_ID   = "domain_fact";
  private static final String                         sandDomainsPrefix   =
      "lu.uni.adtool.domains.sandpredefined";
  private static final String                         oldAdtDomainsPrefix =
      "lu.uni.adtool.domains.predefined";
  private static final String                         adtDomainsPrefix    =
      "lu.uni.adtool.domains.adtpredefined";
  private MainController                              controller;
  private HashMap<Integer, ArrayList<DomainDockable>> domainDockables;
  private HashMap<Integer, Integer>                   domainIdCount;

}
