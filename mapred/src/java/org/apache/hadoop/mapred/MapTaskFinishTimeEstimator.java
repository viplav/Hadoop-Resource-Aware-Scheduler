package org.apache.hadoop.mapred;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;

@InterfaceAudience.Private
@InterfaceStability.Unstable
public class MapTaskFinishTimeEstimator {
    public static final Log LOG = LogFactory.getLog(MapTaskFinishTimeEstimator.class);
    public static final long UNAVAILABLE = -1L;

    MapSampleReport sampleReport;

    long estimatedCpuTime = UNAVAILABLE;
    long estimatedDiskTime = UNAVAILABLE;
    long estimatedNetworkTime = UNAVAILABLE;
    long estimatedFinishTime = UNAVAILABLE;

    long currentTrackerCpuScore = UNAVAILABLE;
    long currentTrackerDiskReadScore = UNAVAILABLE;
    long currentTrackerDiskWriteScore = UNAVAILABLE;
    long currentTrackerNetworkScore = UNAVAILABLE;

    long currentDiskReadSize = UNAVAILABLE;
    long currentDiskWriteSize = UNAVAILABLE;
    long currentNetworkReadSize = UNAVAILABLE;
    long currentNetworkWriteSize = UNAVAILABLE;

    boolean readLocal;
    float localReducePercent;

    //task info
    public MapTaskFinishTimeEstimator(MapSampleReport sampleReport) {
        this.sampleReport = sampleReport;

    }

    //tracker info
    public void setCurrentTrackerResourceScores(TaskTrackerStatus.ResourceStatus resourceStatus) {
        currentTrackerCpuScore = resourceStatus.calculateCPUScore();
        currentTrackerDiskReadScore = resourceStatus.getDiskReadScore();
        currentTrackerDiskWriteScore = resourceStatus.getDiskWriteScore();
        currentTrackerNetworkScore = resourceStatus.getNetworkScore();
    }

    //tracker dependent task info
    public void setLocalReducePercent(float localReducePercent) {
        this.localReducePercent = localReducePercent;
    }

    public void setReadSizes(long inputSize, boolean readLocal){
        this.readLocal = readLocal;
        if(this.readLocal){
            currentDiskReadSize = inputSize;
            currentNetworkReadSize = 0;
        }
        else{
            currentNetworkReadSize = inputSize;
            currentDiskReadSize = 0;
        }
    }

    //disk IO estimation
    private void estimateDiskWriteSize(){
        long sampleReadBytes = sampleReport.getDiskReadBytes() + sampleReport.getNetworkReadBytes();
        long currentReadBytes = currentDiskReadSize + currentNetworkReadSize;
        currentDiskWriteSize = (long)(sampleReport.getDiskWriteBytes() * (1.0 * currentReadBytes/sampleReadBytes));
    }

    private void estimateDiskTime(){
        long sampleIOTime = sampleReport.getDiskReadDurationMilliSec() + sampleReport.getDiskWriteDurationMilliSec()
                + sampleReport.getAdditionalSpillDurationMilliSec();

        estimatedDiskTime = estimateDiskReadTime() + estimateDiskWriteTime();
        LOG.info(sampleReport.getSampleMapTaskId() + "[Disk] sample: " + sampleIOTime + "; estimated: " + estimatedDiskTime);
    }

    private long estimateDiskReadTime(){
        long sampleReadTime = sampleReport.getDiskReadDurationMilliSec();
        long sampleReadSize = sampleReport.getDiskReadBytes();
        long currentReadSize = currentDiskReadSize;

        if(currentReadSize == 0 || sampleReadSize == 0)
            return 0;

        long estimated = (long) (sampleReadTime * (1.0 * currentReadSize/sampleReadSize)
                * (1.0 * sampleReport.getTrackerDiskReadScore()/currentTrackerDiskReadScore));
        LOG.info(sampleReport.getSampleMapTaskId() + "[Disk] sample read: " + sampleReadTime + "; estimated read: " + estimated);
        return estimated;
    }

    private long estimateDiskWriteTime(){
        long sampleWriteTime = sampleReport.getDiskWriteDurationMilliSec()
                + sampleReport.getAdditionalSpillDurationMilliSec();
        long sampleWriteSize = sampleReport.getDiskWriteBytes();
        long currentWriteSize = currentDiskWriteSize;

        if(currentWriteSize == 0 || sampleWriteSize == 0)
            return 0;

        long estimated = (long) (sampleWriteTime * (1.0 * currentWriteSize/sampleWriteSize)
                * (1.0 * sampleReport.getTrackerDiskWriteScore()/currentTrackerDiskWriteScore));
        LOG.info(sampleReport.getSampleMapTaskId() + "[Disk] sample write: " + sampleWriteTime + "; estimated write: " + estimated);
        return estimated;
    }

    //network IO estimation
    private void estimateNetworkWriteSize(){
        if(currentDiskWriteSize == UNAVAILABLE)
            estimateDiskWriteSize();

//        currentNetworkWriteSize = (long)((1-localReducePercent) * currentDiskWriteSize);
         currentNetworkWriteSize = (long)(currentDiskWriteSize/(1.0*sampleReport.getDiskWriteBytes()/sampleReport.getNetworkWriteBytes())); //currentDW-size/partitions
    }

    private void estimateNetworkTime(){
        long sampleIOTime = sampleReport.getNetworkReadDurationMilliSec() + sampleReport.getNetworkWriteDurationMilliSec();
        long sampleIOSize = sampleReport.getNetworkReadBytes() + sampleReport.getNetworkWriteBytes();
        long currentIOSize = currentNetworkReadSize + currentNetworkWriteSize;
        estimatedNetworkTime = (long) (sampleIOTime * (1.0 * currentIOSize/sampleIOSize)
                * (1.0 * sampleReport.getTrackerNetworkIOScore()/currentTrackerNetworkScore));
        LOG.info(sampleReport.getSampleMapTaskId() + "[Network] sample: " + sampleIOTime + "; estimated: " + estimatedNetworkTime);
    }

    //cpu estimation
    private void estimateCPUTime(){
        long sampleCPUTime = sampleReport.getMapDurationMilliSec()
                - sampleReport.getDiskReadDurationMilliSec() - sampleReport.getDiskWriteDurationMilliSec()
                - sampleReport.getAdditionalSpillDurationMilliSec()
                - sampleReport.getNetworkReadDurationMilliSec() - sampleReport.getNetworkWriteDurationMilliSec();
        estimatedCpuTime = (long)((sampleCPUTime<0?0:sampleCPUTime) * (1.0 * sampleReport.getTrackerCPUScore()/currentTrackerCpuScore));
        LOG.info(sampleReport.getSampleMapTaskId() + "[CPU] sample: " + sampleCPUTime + "; estimated: " + estimatedCpuTime);
    }

    private void sumEstimatedTime(){
        estimatedFinishTime = estimatedCpuTime + estimatedDiskTime + estimatedNetworkTime;
    }

    public void estimate(){

        if (!ready() || !sampleReport.ready()){
            LOG.info(sampleReport.getSampleMapTaskId()  + " | estimator ready: " + ready() + "; report ready: "+ sampleReport.ready());
            LOG.info(this.toString());
            return;
        }

        estimateCPUTime();
        estimateDiskWriteSize();  //important: needs to be before the rest of the estimations
        estimateDiskTime();
        estimateNetworkWriteSize();
        estimateNetworkTime();
        sumEstimatedTime();
    }

    private boolean ready(){
        boolean notReady = (localReducePercent==UNAVAILABLE || currentDiskReadSize==UNAVAILABLE || currentNetworkReadSize==UNAVAILABLE
                || currentTrackerCpuScore==UNAVAILABLE || currentTrackerDiskReadScore==UNAVAILABLE || currentTrackerDiskWriteScore==UNAVAILABLE
                || currentTrackerNetworkScore==UNAVAILABLE);
        return !notReady;
    }

    @Override
    public String toString() {
        return "MapTaskFinishTimeEstimator{" +
                ", currentTrackerCpuScore=" + currentTrackerCpuScore +
                ", currentTrackerDiskReadScore=" + currentTrackerDiskReadScore +
                ", currentTrackerDiskWriteScore=" + currentTrackerDiskWriteScore +
                ", currentTrackerNetworkScore=" + currentTrackerNetworkScore +
                ", currentDiskReadSize=" + currentDiskReadSize +
                ", currentDiskWriteSize=" + currentDiskWriteSize +
                ", currentNetworkReadSize=" + currentNetworkReadSize +
                ", currentNetworkWriteSize=" + currentNetworkWriteSize +
                ", readLocal=" + readLocal +
                ", localReducePercent=" + localReducePercent +
                '}';
    }
}
