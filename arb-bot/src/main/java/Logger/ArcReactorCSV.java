package Logger;

import Model.OpportunityCostBreakdown;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class ArcReactorCSV {

    private final BufferedWriter writer;
    private final String filePath;
    private volatile boolean closed = false;

    public ArcReactorCSV(String filePath) throws IOException {
        this.filePath = filePath;
        this.writer = new BufferedWriter(new FileWriter(filePath, true));
        writeHeader();
        System.out.println("CSV logger open: " + filePath);
    }

    private void writeHeader() throws IOException {
        String header = "timestamp,symbol,buyExchange,sellExchange,spotPrice,futuresPrice," +
            "quantity,notionalUSDT,rawSpread,rawSpreadPercent,spotFee,futuresFee,fundingCost," +
            "slippageCost,totalCosts,totalCostsPercent,netProfit,netProfitPercent,fundingRate,dataAgeMs,actionable\n";
        writer.write(header);
        writer.flush();
    }

    public void logOpportunity(OpportunityCostBreakdown breakdown) {
        if (closed) {
            System.err.println("CSV logger is closed");
            return;
        }
        try {
            writer.write(breakdown.toCSVRow() + "\n");
            writer.flush();
        } catch (IOException e) {
            System.err.println("CSV write error: " + e.getMessage());
        }
    }

    public void close() {
        if (!closed) {
            try {
                writer.close();
                closed = true;
                System.out.println("CSV logger closed: " + filePath);
            } catch (IOException e) {
                System.err.println("CSV close error: " + e.getMessage());
            }
        }
    }
}
