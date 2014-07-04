package com.runtimeverification.rvmonitor.java.rvj.output;

import com.runtimeverification.rvmonitor.util.RVMException;
import com.runtimeverification.rvmonitor.java.rvj.output.combinedaspect.CombinedAspect;
import com.runtimeverification.rvmonitor.java.rvj.output.combinedaspect.indexingtree.reftree.RefTree;
import com.runtimeverification.rvmonitor.java.rvj.output.combinedaspect.newindexingtree.IndexingDeclNew;
import com.runtimeverification.rvmonitor.java.rvj.output.monitor.MonitorFeatures;
import com.runtimeverification.rvmonitor.java.rvj.output.monitor.SuffixMonitor;
import com.runtimeverification.rvmonitor.java.rvj.output.monitorset.MonitorSet;
import com.runtimeverification.rvmonitor.java.rvj.parser.ast.RVMSpecFile;
import com.runtimeverification.rvmonitor.java.rvj.parser.ast.mopspec.RVMonitorSpec;
import com.runtimeverification.rvmonitor.java.rvj.parser.ast.mopspec.PropertyAndHandlers;

import java.util.HashMap;
import java.util.TreeMap;

public class AspectJCode {
	private final String name;
	
	private final Package packageDecl;
	private final Imports imports;
	private final TreeMap<RVMonitorSpec, MonitorSet> monitorSets = new TreeMap<RVMonitorSpec, MonitorSet>();
	private final TreeMap<RVMonitorSpec, SuffixMonitor> monitors = new TreeMap<RVMonitorSpec, SuffixMonitor>();
	//Aspect aspect;
	private final CombinedAspect aspect;
	private final TreeMap<RVMonitorSpec, EnableSet> enableSets = new TreeMap<RVMonitorSpec, EnableSet>();
	private final TreeMap<RVMonitorSpec, CoEnableSet> coenableSets = new TreeMap<RVMonitorSpec, CoEnableSet>();
	private boolean versionedStack = false;
	private SystemAspect systemAspect;
	private boolean isCodeGenerated = false;
	
	public AspectJCode(String name, RVMSpecFile rvmSpecFile) throws RVMException {
		this.name = name;
		packageDecl = new Package(rvmSpecFile);
		imports = new Imports(rvmSpecFile);

		for (RVMonitorSpec mopSpec : rvmSpecFile.getSpecs()) {
			EnableSet enableSet = new EnableSet(mopSpec.getEvents(), mopSpec.getParameters());
			CoEnableSet coenableSet = new CoEnableSet(mopSpec.getEvents(), mopSpec.getParameters());

			for (PropertyAndHandlers prop : mopSpec.getPropertiesAndHandlers()) {
				enableSet.add(new EnableSet(prop, mopSpec.getEvents(), mopSpec.getParameters()));
				coenableSet.add(new CoEnableSet(prop, mopSpec.getEvents(), mopSpec.getParameters()));
				versionedStack |= prop.getVersionedStack();
			}

			OptimizedCoenableSet optimizedCoenableSet = new OptimizedCoenableSet(coenableSet);

			enableSets.put(mopSpec, enableSet);
			coenableSets.put(mopSpec, optimizedCoenableSet);

			SuffixMonitor monitor = new SuffixMonitor(mopSpec.getName(), mopSpec, optimizedCoenableSet, true);

			monitors.put(mopSpec, monitor);

			monitorSets.put(mopSpec, new MonitorSet(mopSpec.getName(), mopSpec, monitor));
		}

		//aspect = new Aspect(name, rvmSpecFile, monitorSets, monitors, enableSets, versionedStack);
		aspect = new CombinedAspect(name, rvmSpecFile, monitorSets, monitors, enableSets, versionedStack);

		// Set monitor lock for each monitor set
		for (MonitorSet monitorSet : monitorSets.values()) {
			monitorSet.setMonitorLock(aspect.getAspectName() + "." + aspect.lockManager.getLock().getName());
			monitorSet.setIndexingTreeManager(aspect.indexingTreeManager);
		}

		TreeMap<String, RefTree> refTrees = aspect.indexingTreeManager.refTrees;

		for(SuffixMonitor monitor : monitors.values()){
			monitor.setRefTrees(refTrees);
		}
		
		if(versionedStack)
			systemAspect = new SystemAspect(name);
	}
	
	public void generateCode() {
		if (!this.isCodeGenerated) {
			this.aspect.generateCode();
		}
		
		this.postprocessMonitorFeatures();
	
		this.isCodeGenerated = true;
	}

	private void postprocessMonitorFeatures() {
		for (SuffixMonitor monitor : this.monitors.values()) {
			MonitorFeatures features = monitor.getFeatures();
			features.onCodeGenerationPass1Completed();
		}
	}

	public String toString() {
		// Some non-trivial and potentially side-effect generating works should be done.
		// Since toString() is not a very nice place to do such things, I created a
		// method and let is invoked. I hope such changes are made in many other places.
		this.generateCode();

		String ret = "";

		ret += packageDecl;
		ret += imports;
		ret += "\n";

		for (MonitorSet monitorSet : this.monitorSets.values())
			ret += monitorSet;
		ret += "\n";

		for (SuffixMonitor monitor : this.monitors.values())
			ret += monitor;
		ret += "\n";


		// The order of these two is really important.
		if(systemAspect != null){
			ret += "aspect " + name + "OrderAspect {\n";
			ret += "declare precedence : ";
			ret += systemAspect.getSystemAspectName() + ""; 
			ret += ", ";
			ret += systemAspect.getSystemAspectName() + "2";
			ret += ", ";
			ret += aspect.getAspectName();
			ret += ";\n";
			
			ret += "}\n";
			ret += "\n";
		}

		ret += aspect;

		if(systemAspect != null)
			ret += "\n" + systemAspect;

		return ret;
	}
}
