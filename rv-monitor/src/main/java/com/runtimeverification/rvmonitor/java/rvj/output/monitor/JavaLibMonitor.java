// Handle the creation of java code to be used as a library for the base monitor
// Mostly modified from BaseMonitor.java

package com.runtimeverification.rvmonitor.java.rvj.output.monitor;

import com.runtimeverification.rvmonitor.util.RVMException;
import com.runtimeverification.rvmonitor.java.rvj.output.*;
import com.runtimeverification.rvmonitor.java.rvj.parser.ast.mopspec.EventDefinition;
import com.runtimeverification.rvmonitor.java.rvj.parser.ast.mopspec.RVMonitorSpec;
import com.runtimeverification.rvmonitor.java.rvj.parser.ast.mopspec.PropertyAndHandlers;

import java.util.HashMap;
import java.util.Map.Entry;

public class JavaLibMonitor extends BaseMonitor {
	private final BaseMonitor basemon;
	
	private final PropertyAndHandlers prop;

	public JavaLibMonitor(String name, RVMonitorSpec mopSpec, OptimizedCoenableSet coenableSet, boolean isOutermost)
			throws RVMException {
		super(name, mopSpec, coenableSet, isOutermost);
		
		if(mopSpec.getPropertiesAndHandlers().size() != 1)
			throw new RVMException("JavaLibMonitor does not support multiple properties in one specification");
		
		this.prop = mopSpec.getPropertiesAndHandlers().get(0);
		this.basemon = new BaseMonitor(name, mopSpec, coenableSet, isOutermost);
		this.monitorName = new RVMVariable(mopSpec.getName() + "JavaLibMonitor");
	}

	// Create the event-handling methods, but do not have them accept
	// parameters. This is so as to be able
	// to use these methods to implement the JavaLibInterface in the com.runtimeverification.rvmonitor.java.rt.
	public String doBaseEvent(EventDefinition event) {
		String ret = "";

		PropMonitor propMonitor = propMonitors.get(prop);
		
		int idnum = event.getIdNum();
		RVMJavaCode condition = new RVMJavaCode(event.getCondition(), monitorName);
		RVMJavaCode eventMonitoringCode = new RVMJavaCode(prop, prop.getEventMonitoringCode(event.getId()), monitorName);
		RVMJavaCode monitoringBody = new RVMJavaCode(prop, prop.getLogicProperty("monitoring body"), monitorName);
		HashMap<String, RVMJavaCode> categoryConditions = new HashMap<String, RVMJavaCode>();

		for (String handlerName : prop.getHandlers().keySet()) {
			String conditionStr = prop.getLogicProperty(handlerName + " condition");
			if (conditionStr != null) {
				categoryConditions.put(handlerName, new RVMJavaCodeNoNewLine(conditionStr, monitorName));
			}
		}

		if (event.getAction() != null && event.getAction().getStmts() != null && event.getAction().getStmts().size() != 0) {
			String eventActionStr = event.getAction().toString();

			eventActionStr = eventActionStr.replaceAll("return;", "return true;");
			eventActionStr = eventActionStr.replaceAll("__RESET", "this.reset()");
 			eventActionStr = eventActionStr.replaceAll("__DEFAULT_MESSAGE", defaultMessage);
      //__DEFAULT_MESSAGE may contain __LOC, make sure to sub in __DEFAULT_MESSAGE first
      // -P
			eventActionStr = eventActionStr.replaceAll("__LOC",
					Util.defaultLocation);
			eventActionStr = eventActionStr.replaceAll("__STATICSIG", "this." + staticsig);
			eventActionStr = eventActionStr.replaceAll("__SKIP", skipEvent + " = true");

		}

		// The parameter is omitted so as to be able to implement a more
		// general interface
		ret += "final void event_" + event.getId() + "(" + ") {\n";

		if (!condition.isEmpty()) {
			ret += "if (!(" + condition + ")) {\n";
			ret += "return;\n";
			ret += "}\n";
		}

		if (isOutermost) {
			ret += lastevent + " = " + idnum + ";\n";
		}

		if (monitorInfo != null)
			ret += monitorInfo.union(event.getRVMParametersOnSpec());

		ret += propMonitor.localDeclaration;

		ret += eventMonitoringCode;

		ret += monitoringBody;

		String categoryCode = "";
		for (Entry<String, RVMJavaCode> entry : categoryConditions.entrySet()) {
			categoryCode += propMonitor.categoryVars.get(entry.getKey()) + " = " + entry.getValue() + ";\n";
		}

		if (monitorInfo != null)
			ret += monitorInfo.computeCategory(categoryCode);
		else
			ret += categoryCode;

		// Commented out so as to not actually take any actions on the event
		// if (eventAction != null)
		// ret += "// eventAction\n";
		// ret += eventAction;

		ret += "}\n";

		return ret;
	}

	@Override
	public String toString() {
		boolean hasFail = false;
		boolean hasMatch = false;
		String ret = basemon.toString();

		PropMonitor propMonitor = propMonitors.get(prop);

		ret += "class " + monitorName;
		if (isOutermost)
			ret += " extends " + basemon.monitorName;
		ret += " implements JavaLibInterface, com.runtimeverification.rvmonitor.java.rt.RVMObject {\n";

		// category condition
		for (String category : propMonitor.categoryVars.keySet()) {
			ret += "boolean " + propMonitor.categoryVars.get(category) + " = false;\n";
			if (category.equals("fail"))
				hasFail = true;
			if (category.equals("match"))
				hasMatch = true;
		}
		ret += "\n";

		// constructor
		ret += monitorName + " () {\n";
		ret += "super();\n";
		ret += "}\n";
		ret += "\n";

		// events
		for (EventDefinition event : this.events) {
			ret += "// event: " + event.getId() + "\n";
			ret += this.doBaseEvent(event) + "\n";
		}

		ret += "@Override\n";
		ret += "boolean isCoreachable() {\n";
		// todo: check to make sure this is correct for coreachable
		ret += "return " + (hasFail ? "!Category_fail" : "true") + ";\n";
		ret += "}\n\n";

		ret += "@Override\n";
		ret += "void process(String s) {\n";
		for (EventDefinition event : this.events) {
			String e = event.getId();
			ret += "if (s.equals(\"" + e + "\")) { event_" + e + "(); return; }\n";
		}
		ret += "return;\n";
		ret += "}\n\n";

		ret += "@Override\n";
		ret += "Category getCategory() {\n";
		if (hasMatch)
			ret += "if (Category_match) return Category.Match;\n";
		if (hasFail)
			ret += "if (Category_fail) return Category.Fail;\n";
		ret += "return Category.Unknown;\n";
		ret += "}\n\n";

		ret += "}\n";

		return ret;
	}
}