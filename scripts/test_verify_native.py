"""Regression cases for the binary compatibility gate, using synthetic ELF headers."""
import struct
import unittest
from verify_native import check_elf


def fixture(bits, alignment, file_offset=0, virtual=0):
    data = bytearray(128)
    data[:6] = b'\x7fELF' + bytes([2 if bits == 64 else 1, 1])
    if bits == 64:
        struct.pack_into('<Q', data, 32, 64)
        struct.pack_into('<HH', data, 54, 56, 1)
        struct.pack_into('<IIQQQQQQ', data, 64, 1, 5, file_offset, virtual, 0, 0, 0, alignment)
    else:
        struct.pack_into('<I', data, 28, 52)
        struct.pack_into('<HH', data, 42, 32, 1)
        struct.pack_into('<IIIIIIII', data, 52, 1, file_offset, virtual, 0, 0, 0, 5, alignment)
    return data


class NativeGateTest(unittest.TestCase):
    def test_32_and_64_bit_load_segments_accept_16kb(self):
        for bits in (32, 64):
            check_elf(fixture(bits, 16384, 0x100, 0x4100))

    def test_reject_4kb_and_mismatched_offsets(self):
        for bits in (32, 64):
            with self.assertRaises(RuntimeError):
                check_elf(fixture(bits, 4096))
            with self.assertRaises(RuntimeError):
                check_elf(fixture(bits, 16384, 0x100, 0x4200))


if __name__ == '__main__':
    unittest.main()
