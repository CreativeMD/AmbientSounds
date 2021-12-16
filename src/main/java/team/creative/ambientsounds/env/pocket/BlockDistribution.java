package team.creative.ambientsounds.env.pocket;

public class BlockDistribution {
    
    public double percentage = -1;
    public double count = 0;
    
    public BlockDistribution() {}
    
    public void add(double count) {
        this.count += count;
    }
    
    public void calculatePercentage(double total) {
        this.percentage = this.count / total;
    }
    
    public void add(BlockDistribution dist) {
        this.percentage += dist.percentage;
        this.count += dist.count;
    }
    
    @Override
    public String toString() {
        return "c:" + count + " (" + percentage + ")";
    }
    
}
