package systemoncat.core

import chisel3._
import chisel3.util._
import systemoncat.devices._
import systemoncat.sysbus._

class SysBusRequest extends Bundle {
    val addr = Input(UInt(32.W))
    val data_wr = Input(UInt(32.W))
    val sel = Input(UInt(4.W))
    val wen = Input(Bool())
    val ren = Input(Bool())
}

class SysBusResponse extends Bundle {
    val data_rd = Output(UInt(32.W))
    val locked = Output(Bool())
    val err = Output(Bool())
}

class SysBusBundle extends Bundle {
    val res = new SysBusResponse
    val req = new SysBusRequest
}

class SysBusExternal extends Bundle {
    val ram = Flipped(new SysBusSlaveBundle)
    val serial = Flipped(new SysBusSlaveBundle)
    val irq_client = Flipped(new SysBusSlaveBundle)
    val plic = Flipped(new SysBusSlaveBundle)
    val flash = Flipped(new SysBusSlaveBundle)
    val rom = Flipped(new SysBusSlaveBundle)
}

class SysBusConnectorIO extends Bundle {
    val imem = new SysBusBundle()
    val dmem = new SysBusBundle()
    val external = new SysBusExternal()
}

class SysBusConnector(irq_client: Client, plic: PLIC, rom: ROM) extends Module {
    val io = IO(new SysBusConnectorIO())

    // val bus = Module(new RAMSlaveReflector())
    val ram_slave = Module(new RAMSlaveReflector())
    val serial_slave = Module(new SerialPortSlaveReflector())
    val flash_slave = Module(new FlashSlaveReflector())
    ram_slave.io.in <> io.external.ram
    serial_slave.io.in <> io.external.serial
    flash_slave.io.in <> io.external.flash

    val bus_map = Seq(
        // default -> 0
        BitPat("b00000000000000001111000000000???") -> 1.U(3.W),
        BitPat("b000000100000000001000000000?????") -> 2.U(3.W),
        BitPat("b00000010000000000100000000100???") -> 3.U(3.W),
        BitPat("b0000001000000000010000000011????") -> 4.U(3.W),
        BitPat("b11111111111111111111111111??????") -> 5.U(3.W)
    )
    val bus_slaves: Seq[SysBusSlave] = Array(
        ram_slave,
        serial_slave,
        irq_client,
        plic,
        flash_slave,
        rom
    )

    val bus = Module(new SysBusTranslator(bus_map, bus_slaves))
    bus.io.in(0) <> ram_slave.io.out
    bus.io.in(1) <> serial_slave.io.out
    bus.io.in(2) <> io.external.irq_client
    bus.io.in(3) <> io.external.plic
    bus.io.in(4) <> flash_slave.io.out
    bus.io.in(5) <> io.external.rom

    val imem_en = io.imem.req.wen || io.imem.req.ren
    val dmem_en = io.dmem.req.wen || io.dmem.req.ren
    bus.io.out.dat_i := Mux(io.dmem.req.wen, io.dmem.req.data_wr, 0.U(32.W))
    bus.io.out.adr_i := Mux(dmem_en, io.dmem.req.addr, // data memory first
        Mux(imem_en, io.imem.req.addr, 0.U(32.W)))
    bus.io.out.stb_i := dmem_en || imem_en
    bus.io.out.sel_i := Mux(dmem_en, io.dmem.req.sel,
        Mux(imem_en, io.imem.req.sel, 0.U(32.W)))
    bus.io.out.cyc_i := true.B
    bus.io.out.we_i := io.dmem.req.wen

    io.dmem.res.data_rd := 0.U(32.W)
    io.imem.res.data_rd := 0.U(32.W)
    io.dmem.res.locked := false.B

    val dmem_reg_en = RegInit(false.B)
    val imem_reg_en = RegInit(false.B)

    io.imem.res.locked := dmem_reg_en
    io.dmem.res.err := false.B
    io.imem.res.err := false.B

    dmem_reg_en := dmem_en
    imem_reg_en := imem_en

    when (dmem_reg_en) {
        io.dmem.res.data_rd := bus.io.out.dat_o
        io.imem.res.data_rd := 0.U(32.W)
        io.dmem.res.err := bus.io.out.err_o
        io.imem.res.err := false.B
    }

    when (imem_reg_en && !dmem_reg_en) {
        io.dmem.res.data_rd := 0.U(32.W)
        io.imem.res.data_rd := bus.io.out.dat_o
        io.dmem.res.err := false.B
        io.imem.res.err := bus.io.out.err_o
    }

// class SysBusSlaveBundle extends Bundle{
//     val dat_i = Input(UInt(32.W))
//     val dat_o = Output(UInt(32.W))
//     val ack_o = Output(Bool())
//     val adr_i = Input(UInt(32.W))
//     val cyc_i = Input(Bool())
//     val err_o = Output(Bool())
//     val rty_o = Output(Bool())
//     val sel_i = Input(UInt(4.W))
//     val stb_i = Input(Bool())
//     val we_i = Input(Bool())
// }

}
