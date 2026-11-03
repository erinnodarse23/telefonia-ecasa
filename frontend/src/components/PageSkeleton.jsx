import { useLocation } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';

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

const CardSkeleton = () => (
  <div className="card" style={{ flex: 1, padding: 24 }}>
    <ShimmerBlock width="60%" height={14} mb={20} />
    <ShimmerBlock width="40px" height={32} mb={8} />
    <ShimmerBlock width="100px" height={12} mb={0} />
  </div>
);

const TableHeaderSkeleton = () => (
  <div style={{ display: 'flex', gap: 24, marginBottom: 20 }}>
    <ShimmerBlock width="15%" height={14} mb={0} />
    <ShimmerBlock width="25%" height={14} mb={0} />
    <ShimmerBlock width="20%" height={14} mb={0} />
    <ShimmerBlock width="15%" height={14} mb={0} />
    <ShimmerBlock width="25%" height={14} mb={0} />
  </div>
);

const TableRowsSkeleton = ({ rows = 5 }) => (
  <>
    {Array.from({ length: rows }).map((_, i) => (
      <ShimmerBlock key={i} width="100%" height={14} mb={12} />
    ))}
  </>
);

const TableSkeleton = ({ rows = 5 }) => (
  <div className="card" style={{ flex: 1, padding: 24 }}>
    <TableHeaderSkeleton />
    <TableRowsSkeleton rows={rows} />
  </div>
);

const HeaderSkeleton = () => (
  <div
    style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      marginBottom: 24,
    }}
  >
    <ShimmerBlock width="220px" height={26} />
    <ShimmerBlock width="150px" height={36} mb={0} borderRadius={6} />
  </div>
);

const DashboardSkeleton = () => (
  <>
    <HeaderSkeleton />
    <div style={{ display: 'flex', gap: 20, marginBottom: 24 }}>
      <CardSkeleton />
      <CardSkeleton />
      <CardSkeleton />
    </div>
    <TableSkeleton rows={5} />
  </>
);

const ListSkeleton = () => (
  <>
    <HeaderSkeleton />
    <TableSkeleton rows={5} />
  </>
);

const LoginSkeleton = () => (
  <div
    style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '100vh',
    }}
  >
    <div className="card" style={{ width: '100%', maxWidth: 400, padding: 32 }}>
      <ShimmerBlock width="120px" height={28} mb={28} borderRadius={4} />
      <ShimmerBlock width="100%" height={40} mb={16} />
      <ShimmerBlock width="100%" height={40} mb={24} />
      <ShimmerBlock width="100%" height={42} mb={0} borderRadius={6} />
    </div>
  </div>
);

const PageSkeleton = () => {
  const { theme } = useTheme();
  const location = useLocation();
  const isLight = theme === 'light';

  const bgColor = isLight ? '#f7fafc' : '#0a1a2f';
  const commonStyle = {
    padding: 28,
    backgroundColor: bgColor,
    minHeight: 'calc(100vh - 56px)',
    transition: 'background-color 0.35s ease',
  };

  const path = location.pathname;

  let content;

  if (path === '/login') {
    content = <LoginSkeleton />;
  } else if (path === '/dashboard' || path === '/') {
    content = (
      <div style={commonStyle}>
        <DashboardSkeleton />
      </div>
    );
  } else {
    content = (
      <div style={commonStyle}>
        <ListSkeleton />
      </div>
    );
  }

  return (
    <>
      <style>{shimmerKeyframes}</style>
      {content}
    </>
  );
};

export default PageSkeleton;
