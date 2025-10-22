# Flow Sinh Thời Khóa Biểu (TKB) - Chi Tiết

## Tổng Quan Hệ Thống

Hệ thống sinh TKB được thiết kế theo kiến trúc 2 tầng lưu trữ:
- **Session Storage (Temporary)**: Lưu tạm thời kết quả generate, chưa được xác nhận
- **Global Storage (Permanent)**: Lưu vĩnh viễn sau khi user xác nhận "Thêm vào kết quả"

## 1. Kiến Trúc Tổng Thể

### 1.1 Components Chính

```
Frontend (integrated.html)
    ↓
RoomController (@RestController)
    ↓
TimetableSchedulingService (Business Logic)
    ↓
RoomService (Room Assignment)
    ↓
DataLoaderService (Data & Persistence)
```

### 1.2 State Management

#### Global State Variables:
- **`lastSlotIdx`** (int): Vị trí slot hiện tại - CHỈ update khi user confirm
- **`sessionLastSlotIdx`** (int): Vị trí slot tạm thời trong quá trình generate
- **`sessionOccupiedRooms`** (Set<Object>): Phòng đã dùng trong session hiện tại
- **`globalOccupiedRooms`** (file JSON): Phòng đã dùng được lưu vĩnh viễn

#### Persistence Files:
- `global_occupied_rooms.json`: Lưu danh sách phòng đã sử dụng
- `last_slot_idx.json`: Lưu vị trí slot cuối cùng (deprecated - dùng biến trong code)

---

## 2. Flow Chi Tiết - Sinh TKB

### 2.1 Entry Point: `/api/rooms/simulate-batch` (POST)

**Request Format:**
```json
{
  "items": [
    {
      "ma_mon": "BAS1158",
      "ten_mon": "Tiếng Anh (Course 2)",
      "sotiet": 60,
      "solop": 116,
      "siso": 3944,
      "siso_mot_lop": 34,
      "subject_type": "English",
      "student_year": 2024,
      "he_dac_thu": ""
    }
  ]
}
```

### 2.2 Bước 1: Initialization

```java
// TimetableSchedulingService.simulateExcelFlowBatch()

// 1. Load template data từ real.json
List<TKBTemplateRow> dataRows = dataLoaderService.loadTKBTemplateData();

// 2. Load danh sách phòng
List<RoomData> rooms = dataLoaderService.loadRooms();

// 3. Load global occupied rooms (PERMANENT)
Set<Object> globalOccupiedRooms = loadGlobalOccupiedRooms();

// 4. Tạo working set = global + session
Set<Object> occupiedRooms = new HashSet<>(globalOccupiedRooms);

// 5. Initialize session lastSlotIdx from permanent lastSlotIdx
sessionLastSlotIdx = lastSlotIdx;
```

### 2.3 Bước 2: Sort và Prioritize Subjects

```java
// ƯU TIÊN MÔN 60 TIẾT TRƯỚC
List<TKBRequest> sortedItems = new ArrayList<>(request.getItems());
sortedItems.sort((a, b) -> {
    int aTotal = a.getSotiet();
    int bTotal = b.getSotiet();
    
    // Môn 60 tiết lên đầu
    if (aTotal == 60 && bTotal != 60) return -1;
    if (aTotal != 60 && bTotal == 60) return 1;
    
    // Cùng priority thì sort theo số tiết
    return Integer.compare(aTotal, bTotal);
});
```

### 2.4 Bước 3: Process Từng Môn Học

#### 3.4.1 Filter Template Data
```java
// Lọc data theo số tiết
List<TKBTemplateRow> pool = dataRows.stream()
    .filter(row -> toInt(row.getTotalPeriods()) == targetTotal)
    .collect(Collectors.toList());
```

#### 3.4.2 Calculate Starting Slot
```java
int startingSlotIdx = (sessionLastSlotIdx + 1) % ROTATING_SLOTS.size();
```

#### 3.4.3 Branch: 60-Period vs Regular

**Nếu môn 60 tiết:**
```java
if (targetTotal == 60) {
    classes = Math.max(1, toInt(tkbRequest.getSolop(), 1));
    resultRows = process60PeriodSubject(...);
}
```

**Nếu môn thường:**
```java
else {
    classes = Math.max(1, toInt(tkbRequest.getSolop(), 1));
    resultRows = processRegularSubject(...);
}
```

---

## 3. Logic Môn 60 Tiết (Special Case)

### 3.1 Đặc Điểm

- **60 tiết = 30 tiết/thứ × 2 thứ liên tiếp**
- **2 thứ liên tiếp, cùng kíp, cùng phòng**
- **Mỗi lớp: 4 dòng** (2 dòng/thứ × 2 thứ)

### 3.2 Rotating Slots (12 slots)

```java
ROTATING_SLOTS_60 = [
    (Thứ 2-3, Kíp 1), (Thứ 2-3, Kíp 2),  // Slots 0-1
    (Thứ 4-5, Kíp 3), (Thứ 4-5, Kíp 4),  // Slots 2-3
    (Thứ 6-7, Kíp 1), (Thứ 6-7, Kíp 2),  // Slots 4-5
    (Thứ 2-3, Kíp 3), (Thứ 2-3, Kíp 4),  // Slots 6-7
    (Thứ 4-5, Kíp 1), (Thứ 4-5, Kíp 2),  // Slots 8-9
    (Thứ 6-7, Kíp 3), (Thứ 6-7, Kíp 4)   // Slots 10-11
]
```

### 3.3 Flow Process 60-Period Subject

```java
// 1. Group data by (thu, kip)
Map<String, List<Row>> groups = groupBy("thu_kip");

// 2. Loop qua từng lớp
for (int cls = 1; cls <= classes; cls++) {
    
    // 3. Get slot cho lớp này
    int slotIdx = (startingSlotIdx + (cls - 1)) % 12;
    DayPairSlot slot = ROTATING_SLOTS_60.get(slotIdx);
    
    // Ví dụ: slot = (Thứ 2-3, Kíp 1)
    Integer targetKip = slot.getKip(); // = 1
    
    // 4. Pick ONE room cho cả 2 thứ
    String classRoomCode = null;
    
    // 5. Process cả 2 thứ với cùng kíp
    for (Integer day : [day1, day2]) {  // [2, 3]
        
        // Get rows cho (thu=day, kip=1)
        List<Row> groupRows = groups.get(day + "_" + targetKip);
        
        // 6. Process từng row trong group (~2 rows = 30 tiết)
        for (Row row : groupRows) {
            
            // Pick room lần đầu tiên gặp
            if (classRoomCode == null) {
                classRoomCode = pickRoom(...);
            }
            
            // Mark occupied cho CẢ 2 thứ
            occupiedRooms.add(roomCode + "|" + day1 + "|" + kip);
            occupiedRooms.add(roomCode + "|" + day2 + "|" + kip);
            
            // Emit row
            emitRow(cls, tkbRequest, row, ah, classRoomCode);
        }
    }
}
```

### 3.4 Ví Dụ Cụ Thể (116 lớp)

**Input:**
- Môn: BAS1158 - Tiếng Anh (Course 2)
- Số tiết: 60
- Số lớp: 116

**Output:**
```
Lớp 1:  Thứ 2-3, Kíp 1, Phòng G01-A2 (4 dòng)
Lớp 2:  Thứ 2-3, Kíp 2, Phòng G02-A1 (4 dòng)
Lớp 3:  Thứ 4-5, Kíp 3, Phòng G03-B2 (4 dòng)
Lớp 4:  Thứ 4-5, Kíp 4, Phòng G04-C1 (4 dòng)
...
Lớp 116: Thứ 6-7, Kíp 4, Phòng G05-D3 (4 dòng)

Tổng: 116 × 4 = 464 dòng
```

---

## 4. Logic Môn Thường (Regular Subjects)

### 4.1 Rotating Slots (12 slots)

```java
ROTATING_SLOTS = [
    (Thứ 2, sáng), (Thứ 3, chiều),   // Slots 0-1
    (Thứ 4, sáng), (Thứ 5, chiều),   // Slots 2-3
    (Thứ 6, sáng), (Thứ 7, chiều),   // Slots 4-5
    (Thứ 2, chiều), (Thứ 3, sáng),   // Slots 6-7
    (Thứ 4, chiều), (Thứ 5, sáng),   // Slots 8-9
    (Thứ 6, chiều), (Thứ 7, sáng)    // Slots 10-11
]
```

### 4.2 Slot Distribution Strategy

```java
// Môn 14 tiết: 4 lớp/slot
if (targetTotal == 14) {
    slotIdx = (startingSlotIdx + (cls - 1) / 4) % 12;
}
// Các môn khác: 2 lớp/slot
else {
    slotIdx = (startingSlotIdx + (cls - 1) / 2) % 12;
}
```

### 4.3 Flow Process Regular Subject

```java
// 1. Loop qua từng lớp
for (int cls = 1; cls <= classes; cls++) {
    
    // 2. Calculate slot cho lớp này
    int slotIdx = calculateSlotIdx(cls, targetTotal, startingSlotIdx);
    TimetableSlot slot = ROTATING_SLOTS.get(slotIdx);
    
    // Ví dụ: slot = (Thứ 2, sáng) → kips = {1, 2}
    Set<Integer> targetKips = slot.getKipSet();
    
    // 3. Pick ONE room cho lớp này
    String classRoomCode = null;
    
    // 4. Distribute periods (ai = targetTotal)
    int ai = targetTotal;
    int idx = 0;
    
    while (ai > 0 && guard < 10000) {
        
        // 5. Find matching row: (thu=2, kip in {1,2})
        Row row = findMatchingRow(pool, slot.getThu(), targetKips);
        
        // 6. Calculate AH (periods in this row)
        int ah = calculateAH(row);
        
        // 7. Pick room on first valid row
        if (classRoomCode == null && tiet_bd != 12) {
            classRoomCode = pickRoom(...);
            
            // Mark occupied
            occupiedRooms.add(roomCode + "|" + thu + "|" + kip);
        }
        
        // 8. Emit row
        emitRow(cls, tkbRequest, row, ai, classRoomCode);
        
        // 9. Subtract periods
        ai -= ah;
        guard++;
    }
}
```

---

## 5. Room Assignment Logic (pickRoom)

### 5.1 Input Parameters

```java
RoomPickResult pickRoom(
    List<RoomData> rooms,           // Danh sách phòng
    Integer sisoPerClass,           // Sĩ số mỗi lớp
    Set<Object> occupiedRooms,      // Phòng đã dùng
    Integer thu,                    // Thứ (2-7)
    Integer kip,                    // Kíp (1-4)
    String subjectType,             // Loại môn: "English", "CLC", "NT", null
    Integer studentYear,            // Năm sinh viên
    String heDacThu,                // Hệ đặc thù
    List<Integer> weekSchedule      // Tuần học (optional)
)
```

### 5.2 Room Selection Priority

```
Priority 1: Phòng chuyên dụng theo loại môn
    - English → Phòng có "ENGLISH" trong mã
    - CLC → Phòng có "CLC" 
    - NT → Phòng có "NT"

Priority 2: Phòng theo năm
    - K1/K2 (năm 1-2) → Các phòng nhỏ
    - K3/K4 (năm 3-4) → Các phòng lớn hơn

Priority 3: Capacity
    - Chọn phòng có sức chứa >= siso
    - Ưu tiên phòng vừa đủ (tránh lãng phí)

Priority 4: Availability
    - Phòng chưa bị occupied tại (thu, kip)
```

### 5.3 Room Pick Algorithm

```java
// 1. Filter available rooms
List<RoomData> availableRooms = rooms.stream()
    .filter(room -> {
        String occupationKey = room.getRoomCode() + "|" + thu + "|" + kip;
        return !occupiedRooms.contains(occupationKey);
    })
    .collect(Collectors.toList());

// 2. Apply subject type constraints
if ("English".equals(subjectType)) {
    availableRooms = availableRooms.stream()
        .filter(r -> r.getRoomCode().contains("ENGLISH"))
        .collect(Collectors.toList());
}
else if ("CLC".equals(subjectType)) {
    availableRooms = availableRooms.stream()
        .filter(r -> r.getRoomCode().contains("CLC"))
        .collect(Collectors.toList());
}
else if ("NT".equals(subjectType)) {
    availableRooms = availableRooms.stream()
        .filter(r -> r.getRoomCode().contains("NT"))
        .collect(Collectors.toList());
}

// 3. Filter by capacity
availableRooms = availableRooms.stream()
    .filter(r -> r.getCapacity() >= sisoPerClass)
    .collect(Collectors.toList());

// 4. Sort by capacity (ascending - best fit)
availableRooms.sort(Comparator.comparingInt(RoomData::getCapacity));

// 5. Pick first available room
if (!availableRooms.isEmpty()) {
    RoomData selectedRoom = availableRooms.get(0);
    return RoomPickResult.success(
        selectedRoom.getRoomCode(),
        selectedRoom.getMaPhong()
    );
}

// 6. No room available
return RoomPickResult.noRoom();
```

### 5.4 Occupation Key Format

```
Format: "{roomCode}|{thu}|{kip}"

Examples:
- "G01-A2|2|1"  → Phòng G01-A2, Thứ 2, Kíp 1
- "301-NT|4|3"  → Phòng 301-NT, Thứ 4, Kíp 3
- "CLC-B1|7|2"  → Phòng CLC-B1, Thứ 7, Kíp 2
```

**Quan trọng:** Occupation key chỉ bao gồm (room, thu, kip), KHÔNG bao gồm `tiet_bd`. Điều này có nghĩa:
- Một phòng bị occupy cho cả kíp đó
- Ví dụ: Nếu phòng G01-A2 dùng cho Thứ 2, Kíp 1, Tiết 1-2 → cả kíp 1 (tiết 1-6) đều bị occupied

---

## 6. Major End Slot Calculation

### 6.1 Logic

```java
private int calculateMajorEndSlot(int classes, int targetTotal) {
    if (targetTotal == 14) {
        // 14 tiết: 4 lớp/slot
        return (classes - 1) / 4;
    } else {
        // Các môn khác: 2 lớp/slot
        return (classes - 1) / 2;
    }
}
```

### 6.2 Examples

```
Môn 14 tiết, 10 lớp:
    endSlot = (10-1)/4 = 9/4 = 2
    → Dùng slots: 0,1,2 (3 slots tổng cộng)

Môn 30 tiết, 8 lớp:
    endSlot = (8-1)/2 = 7/2 = 3
    → Dùng slots: 0,1,2,3 (4 slots tổng cộng)
```

---

## 7. Session Management & Persistence

### 7.1 Session Flow (Temporary)

```
1. User clicks "Sinh TKB"
   ↓
2. simulateExcelFlowBatch() executes
   ↓
3. sessionOccupiedRooms populated
   ↓
4. sessionLastSlotIdx calculated
   ↓
5. Return results to frontend (preview)
```

**State tại thời điểm này:**
- `sessionOccupiedRooms`: Có data
- `sessionLastSlotIdx`: Đã update
- `globalOccupiedRooms`: KHÔNG thay đổi
- `lastSlotIdx`: KHÔNG thay đổi

### 7.2 Commit Flow (Permanent)

```
1. User clicks "Thêm vào kết quả"
   ↓
2. POST /api/rooms/save-results
   ↓
3. commitSessionToGlobal() executes
   ↓
4. sessionOccupiedRooms → globalOccupiedRooms
   ↓
5. sessionLastSlotIdx → lastSlotIdx
   ↓
6. Save to global_occupied_rooms.json
   ↓
7. Clear sessionOccupiedRooms
```

**Code:**
```java
public void commitSessionToGlobal() {
    // Load global
    Set<Object> globalOccupied = loadGlobalOccupiedRooms();
    
    // Merge session into global
    globalOccupied.addAll(sessionOccupiedRooms);
    
    // Persist to JSON
    saveGlobalOccupiedRooms(globalOccupied);
    
    // COMMIT lastSlotIdx
    lastSlotIdx = sessionLastSlotIdx;
    
    // Clear session
    sessionOccupiedRooms.clear();
}
```

### 7.3 Reset Flow

```
1. User clicks "Reset phòng đã sử dụng"
   ↓
2. POST /api/rooms/reset
   ↓
3. resetOccupiedRooms() executes
   ↓
4. Clear sessionOccupiedRooms
   ↓
5. Clear globalOccupiedRooms (file)
   ↓
6. Reset lastSlotIdx = -1
   ↓
7. Reset sessionLastSlotIdx = -1
```

**Code:**
```java
public void resetOccupiedRooms() {
    // Clear session
    sessionOccupiedRooms.clear();
    
    // Clear global file
    Set<Object> emptySet = new HashSet<>();
    saveGlobalOccupiedRooms(emptySet);
    
    // Reset indices
    lastSlotIdx = -1;
    sessionLastSlotIdx = -1;
}
```

---

## 8. API Endpoints

### 8.1 Generate TKB (Main)

```http
POST /api/rooms/simulate-batch
Content-Type: application/json

{
  "items": [...]
}
```

**Response:**
```json
{
  "items": [
    {
      "input": {...},
      "rows": [
        {
          "lop": 1,
          "ma_mon": "BAS1158",
          "ten_mon": "Tiếng Anh (Course 2)",
          "thu": 2,
          "kip": 1,
          "tiet_bd": 1,
          "L": 2,
          "phong": "G01-A2",
          "ma_phong": "301-NT",
          "weeks": [1,2,3,4,5,6,7,8]
        }
      ]
    }
  ],
  "totalRows": 464,
  "lastSlotIdx": 8,
  "occupiedRoomsCount": 116
}
```

### 8.2 Commit Results

```http
POST /api/rooms/save-results
```

**Response:**
```json
{
  "success": true,
  "message": "Results saved successfully"
}
```

### 8.3 Reset Occupied Rooms

```http
POST /api/rooms/reset
```

**Response:**
```json
{
  "success": true,
  "message": "Occupied rooms reset successfully"
}
```

### 8.4 Get Occupied Rooms Info

```http
GET /api/rooms/occupied-info
```

**Response:**
```json
{
  "session": 116,
  "global": 450,
  "total": 566
}
```

---

## 9. Data Structures

### 9.1 TKBTemplateRow (real.json)

```json
[60, 2, 1, 1, 2, "1761154302217.6123", 1, 1, 1, 0, 1, 1, 1, 1]
```

**Mapping:**
```
[0] = 60  → totalPeriods
[1] = 2   → dayOfWeek (thu)
[2] = 1   → kip
[3] = 1   → startPeriod (tiet_bd)
[4] = 2   → L (số tiết liên tiếp)
[5] = id
[6..13] → weeks (tuần 1-8)
```

### 9.2 RoomData (rooms.json)

```json
{
  "roomCode": "G01-A2",
  "maPhong": "301-NT",
  "capacity": 40,
  "type": "regular"
}
```

### 9.3 Global Occupied Rooms (global_occupied_rooms.json)

```json
[
  "G01-A2|2|1",
  "G01-A2|3|1",
  "G02-B1|4|3",
  "301-NT|2|2",
  ...
]
```

---

## 10. Edge Cases & Special Handling

### 10.1 Tiết 12 (No Room Assignment)

```java
if (tiet_bd != null && tiet_bd == 12) {
    // Không gán phòng cho tiết 12
    rowRoomCode = null;
    rowRoomMaPhong = null;
}
```

**Lý do:** Tiết 12 là giờ nghỉ trưa, không cần phòng học.

### 10.2 Hệ Đặc Thù (Special System)

```java
if (heDacThu != null && !heDacThu.trim().isEmpty()) {
    // Tính sĩ số mỗi lớp = tổng sĩ số / số lớp
    sisoPerClass = siso / solop;
} else {
    // Dùng sĩ số mỗi lớp có sẵn
    sisoPerClass = siso_mot_lop;
}
```

### 10.3 Empty Pool

```java
if (pool.isEmpty()) {
    itemsOut.add(TKBBatchItemResponse.builder()
        .input(tkbRequest)
        .rows(Collections.emptyList())
        .note("Không có Data cho " + targetTotal + " tiết")
        .build());
    continue;
}
```

### 10.4 Insufficient Data

```java
if (ai > 0) {
    log.warn("Not enough data to schedule all classes (remaining: {})", ai);
    break;
}
```

---

## 11. Testing Scenarios

### 11.1 Scenario 1: Single Regular Subject

**Input:**
- 1 môn, 30 tiết, 4 lớp

**Expected:**
- 4 lớp × ~3 dòng/lớp = 12 dòng
- Dùng 2 slots (2 lớp/slot)
- 4 phòng khác nhau (1 phòng/lớp)

### 11.2 Scenario 2: Single 60-Period Subject

**Input:**
- 1 môn, 60 tiết, 10 lớp

**Expected:**
- 10 lớp × 4 dòng/lớp = 40 dòng
- Dùng 10 slots (1 lớp/slot)
- 10 phòng khác nhau
- Mỗi lớp: 2 thứ liên tiếp, cùng kíp, cùng phòng

### 11.3 Scenario 3: Mixed Subjects

**Input:**
- Môn A: 60 tiết, 5 lớp
- Môn B: 30 tiết, 8 lớp
- Môn C: 14 tiết, 12 lớp

**Expected:**
- Môn A xếp trước (priority)
- Slots cascade: A → B → C
- Room occupation không conflict

### 11.4 Scenario 4: Generate → Re-generate (Without Commit)

**Steps:**
1. Generate TKB → sessionLastSlotIdx = 5
2. Generate lại → vẫn bắt đầu từ lastSlotIdx = -1
3. sessionLastSlotIdx = 5 (giống lần 1)

**Expected:**
- lastSlotIdx không thay đổi
- Kết quả giống hệt nhau

### 11.5 Scenario 5: Generate → Commit → Generate

**Steps:**
1. Generate TKB → sessionLastSlotIdx = 5
2. Commit → lastSlotIdx = 5
3. Generate TKB mới → bắt đầu từ slot 6

**Expected:**
- lastSlotIdx = 5 (permanent)
- Generation tiếp theo dùng slot 6+

---

## 12. Performance Considerations

### 12.1 Time Complexity

- **Filter pool:** O(n) - n = số dòng trong real.json
- **Find matching row:** O(m) - m = kích thước pool
- **Pick room:** O(r) - r = số phòng available
- **Overall per class:** O(n + m + r)
- **Overall per subject:** O(classes × (n + m + r))

### 12.2 Memory Usage

- **Template data:** ~5000 rows × 14 fields = ~70KB
- **Rooms:** ~200 rooms × 4 fields = ~1KB
- **Occupied rooms:** ~1000 occupations × 20 bytes = ~20KB
- **Total:** ~100KB per generation

### 12.3 Optimization Tips

1. **Cache template data:** Load once at startup
2. **Index rooms by type:** Pre-filter English/CLC/NT rooms
3. **Use HashSet for occupied:** O(1) lookup
4. **Limit guard iterations:** Prevent infinite loops

---

## 13. Troubleshooting

### 13.1 "Không có Data cho X tiết"

**Cause:** Template data thiếu dòng cho số tiết này

**Solution:** Kiểm tra real.json có dòng nào có totalPeriods = X không

### 13.2 "Không đủ Data để xếp hết lớp"

**Cause:** Pool không đủ rows để đạt targetTotal × classes

**Solution:** Tăng dữ liệu template hoặc giảm số lớp

### 13.3 Phòng không được gán

**Cause:** 
- Tất cả phòng đã occupied
- Không có phòng đủ capacity
- Subject type không match phòng có sẵn

**Solution:** 
- Reset occupied rooms
- Thêm phòng mới
- Kiểm tra room constraints

### 13.4 Slot không rotate đúng

**Cause:** lastSlotIdx không được commit

**Solution:** Bấm "Thêm vào kết quả" để commit

---

## 14. Future Enhancements

### 14.1 Planned Features

1. **Week-based occupation:** Occupation theo tuần cụ thể
2. **Room preferences:** User chọn phòng ưu tiên
3. **Conflict detection:** Báo conflict trước khi generate
4. **Undo/Redo:** Hoàn tác thao tác
5. **Export to Excel:** Xuất TKB ra file

### 14.2 Code Improvements

1. **Extract constants:** Move magic numbers to config
2. **Add unit tests:** Test từng function riêng lẻ
3. **Parallel processing:** Generate nhiều môn song song
4. **Database persistence:** Thay JSON bằng DB

---

## 15. References

### 15.1 Key Files

- **Backend:**
  - `TimetableSchedulingService.java` - Core logic
  - `RoomService.java` - Room assignment
  - `DataLoaderService.java` - Data persistence
  - `RoomController.java` - API endpoints

- **Frontend:**
  - `integrated.html` - UI
  
- **Data:**
  - `real.json` - Template data
  - `rooms.json` - Room list
  - `global_occupied_rooms.json` - Occupied rooms

### 15.2 Related Documents

- `API_DOCUMENTATION.md` - API reference
- `README.md` - Project setup

---

## 16. Contact & Support

**Maintainer:** Development Team  
**Last Updated:** 2025-01-23  
**Version:** 1.0.0

For issues or questions, please refer to the project repository or contact the development team.
