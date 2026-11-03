const shimmerKeyframes = `
@keyframes sk-shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
`;

const ShimmerBlock = ({ width, height, mb, borderRadius }) => (
  <div
    style={{
      width: width || '100%',
      height: height || 16,
      borderRadius: borderRadius ?? 6,
      background: 'linear-gradient(90deg, var(--border) 25%, var(--card-bg) 50%, var(--border) 75%)',
      backgroundSize: '200% 100%',
      animation: 'sk-shimmer 2s ease-in-out infinite',
      marginBottom: mb ?? 16,
    }}
  />
);

const TableHeaderSkeleton = ({ columns = 5 }) => (
  <thead>
    <tr>
      {Array.from({ length: columns }).map((_, i) => (
        <th key={i}>
          <ShimmerBlock width={`${Math.floor(60 + Math.random() * 40)}%`} height={14} mb={0} />
        </th>
      ))}
    </tr>
  </thead>
);

const TableRowSkeleton = ({ columns = 5, index = 0 }) => (
  <tr style={{ animation: `fadeInUp 0.25s ease ${index * 0.025}s both` }}>
    {Array.from({ length: columns }).map((_, i) => (
      <td key={i}>
        <ShimmerBlock
          width={`${Math.floor(50 + Math.random() * 50)}%`}
          height={14}
          mb={0}
        />
      </td>
    ))}
  </tr>
);

const SkeletonLoader = ({ rows = 5, columns = 5 }) => (
  <>
    <style>{shimmerKeyframes}</style>
    <div className="table-container" style={{ animation: 'fadeInUp 0.35s ease' }}>
      <div className="table-responsive">
        <table>
          <TableHeaderSkeleton columns={columns} />
          <tbody>
            {Array.from({ length: rows }).map((_, i) => (
              <TableRowSkeleton key={i} columns={columns} index={i} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  </>
);

export default SkeletonLoader;
