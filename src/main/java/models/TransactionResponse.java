package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponse extends BaseModel {
    private int id;
    private double amount;
    private String type;
    private String timestamp;
    private int relatedAccountId;
}
