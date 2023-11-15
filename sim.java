import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.network.datacenter.NetworkDatacenter;
import org.cloudbus.cloudsim.network.datacenter.NetworkDatacenterCharacteristics;
import org.cloudbus.cloudsim.network.datacenter.NetworkVm;
import org.cloudbus.cloudsim.network.datacenter.NetworkVmAllocationPolicy;
import org.cloudbus.cloudsim.network.datacenter.NetworkVmAllocationPolicySimple;
import org.cloudbus.cloudsim.network.datacenter.NetworkVmSchedulerTimeShared;
import org.cloudbus.cloudsim.network.datacenter.TaskStage;
import org.cloudbus.cloudsim.network.datacenter.TaskStageEvent;
import org.cloudbus.cloudsim.network.datacenter.VirtualMachineState;
import org.cloudbus.cloudsim.network.datacenter.VmPacket;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RLSVNESimulator {

    private static final int NUM_SUBSTRATE_NODES = 10;
    private static final int NUM_VIRTUAL_NODES = 5;

    private List<NetworkVm> substrateNodes;
    private List<NetworkVm> virtualNodes;

    public RLSVNESimulator() {
        initializeSubstrateNodes();
        initializeVirtualNodes();
    }

    private void initializeSubstrateNodes() {
        substrateNodes = new ArrayList<>();
        for (int i = 0; i < NUM_SUBSTRATE_NODES; i++) {
            NetworkVm node = new NetworkVm(i, 1000, 1, 1, 1, new UtilizationModelFull(), new UtilizationModelDynamic());
            substrateNodes.add(node);
        }
    }

    private void initializeVirtualNodes() {
        virtualNodes = new ArrayList<>();
        for (int i = 0; i < NUM_VIRTUAL_NODES; i++) {
            NetworkVm node = new NetworkVm(i + NUM_SUBSTRATE_NODES, 500, 1, 1, 1, new UtilizationModelFull(), new UtilizationModelDynamic());
            virtualNodes.add(node);
        }
    }

    public Map<NetworkVm, NetworkVm> RLSVNE() {
        Map<NetworkVm, NetworkVm> mapping = new HashMap<>();

        for (NetworkVm vnNode : virtualNodes) {
            double maxLrd = 0;
            NetworkVm maxSnNode = null;

            for (NetworkVm snNode : substrateNodes) {
                if (snNode.getState() == VirtualMachineState.RUNNING
                        && snNode.getCurrentRequestedMips() >= vnNode.getCurrentRequestedMips()
                        && snNode.getCurrentAllocatedBw() >= 1) {
                    double lrd = calculateLinkRelationDegree(vnNode, snNode);

                    if (lrd > maxLrd) {
                        maxLrd = lrd;
                        maxSnNode = snNode;
                    }
                }
            }

            if (maxSnNode != null) {
                mapping.put(vnNode, maxSnNode);
                maxSnNode.setCurrentRequestedMips(maxSnNode.getCurrentRequestedMips() - vnNode.getCurrentRequestedMips());
                maxSnNode.setCurrentAllocatedBw(maxSnNode.getCurrentAllocatedBw() - 1);
            }
        }

        return mapping;
    }

    private double calculateLinkRelationDegree(NetworkVm vnNode, NetworkVm snNode) {
        double sil = 0;

        if (snNode.getVmPacketHandler() != null) {
            for (VmPacket packet : snNode.getVmPacketHandler().getVmPacketList()) {
                if (packet.getDestId() == vnNode.getId()) {
                    sil++;
                }
            }
        }

        return (sil + 1) * (snNode.getCurrentAllocatedBw() / snNode.getCurrentRequestedMips());
    }

    public static void main(String[] args) {
        RLSVNESimulator simulator = new RLSVNESimulator();
        Map<NetworkVm, NetworkVm> mapping = simulator.RLSVNE();

        System.out.println("Virtual Node to Substrate Node Mapping:");
        for (Map.Entry<NetworkVm, NetworkVm> entry : mapping.entrySet()) {
            System.out.println("Virtual Node " + entry.getKey().getId() + " -> Substrate Node " + entry.getValue().getId());
        }
    }
}


