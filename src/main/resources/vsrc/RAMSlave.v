module RAMSlave(dat_i, dat_o, ack_o, adr_i, adr_i, cyc_i,
    err_o, rty_o, sel_i, stb_i, we_i,
    sram_adr, sram_dat, sram_ce, sram_oe,
    sram_we, sram_lb, sram_ub, clock_bus, rst_bus, clk_ram, rst_ram);

input clk_bus;
input rst_bus;

// ----------- system bus slave interface ---------

input clk_ram;
input rst_ram;

input [31:0] dat_i;
output [31:0] dat_o;
output ack_o;
input [31:0] adr_i;
input cyc_i;
output err_o;
output rty_o;
input [3:0] sel_i;
input stb_i;
input we_i;

// ------------ SRAM interface --------------
output [19:0] sram_adr;
inout [31:0] sram_dat;
output sram_ce;
output sram_oe;
output sram_we;
output sram_lb;
output sram_ub;

reg [31:0] stored_dat;


localparam STATE_IDLE = 3'b000,
    STATE_READ = 3'b001;
    STATE_WRITE = 3'b010;

wire [2:0] state;

always@(posedge clk_ram) begin
    case(state)
        STATE_IDLE: begin
            ack_o = 1'b0;
            if (cyc_i and stb_i) begin
                // transaction cycle requested
                if(we_i) begin
                    stored_dat <= dat_i;
                    state = STATE_WRITE;
                end
                else
                    state = STATE_READ;
            end
        end
        STATE_WRITE: begin
            stored_dat <= sram_dat;
            ack_o = 1'b1;
            state = STATE_IDLE;
        end
        STATE_READ: begin
            ack_o = 1'b1;
            state = STATE_IDLE;
        end
    endcase
end

always@* begin
    case(state)
        STATE_IDLE: begin
            sram_ce = 1'b1;
        end
        STATE_READ: begin
            sram_we = 1'b1;
            sram_dat = {32{1'Z}};
            sram_ce = 1'b0;
        end
        STATE_WRITE: begin
            sram_we = 1'b0;
            sram_dat = stored_dat;
            sra_ce = 1'b0;
        end
    endcase

    sram_lb = 1'b0;
    sram_ub = 1'b0;
    sram_oe = 1'b0;
    dat_o = stored_dat;

    err_o = 1'b0;
    rty_o = 1'b0;
end

endmodule
